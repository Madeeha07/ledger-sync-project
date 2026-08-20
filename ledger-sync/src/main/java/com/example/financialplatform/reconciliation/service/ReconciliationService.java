package com.example.financialplatform.reconciliation.service;

import com.example.financialplatform.exception.InvalidCsvException;
import com.example.financialplatform.exception.ResourceNotFoundException;
import com.example.financialplatform.reconciliation.dto.ReconciliationResultResponse;
import com.example.financialplatform.reconciliation.dto.ReconciliationRunResponse;
import com.example.financialplatform.reconciliation.entity.ReconciliationResult;
import com.example.financialplatform.reconciliation.entity.ReconciliationResultType;
import com.example.financialplatform.reconciliation.entity.ReconciliationRun;
import com.example.financialplatform.reconciliation.entity.SettlementRecord;
import com.example.financialplatform.reconciliation.repository.ReconciliationResultRepository;
import com.example.financialplatform.reconciliation.repository.ReconciliationRunRepository;
import com.example.financialplatform.reconciliation.repository.SettlementRecordRepository;
import com.example.financialplatform.transaction.entity.FinancialTransaction;
import com.example.financialplatform.transaction.entity.TransactionStatus;
import com.example.financialplatform.transaction.repository.FinancialTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReconciliationService {

    private static final long MAX_FILE_SIZE = 5L * 1024L * 1024L;

    private final ReconciliationRunRepository runRepository;
    private final SettlementRecordRepository settlementRecordRepository;
    private final ReconciliationResultRepository resultRepository;
    private final FinancialTransactionRepository transactionRepository;

    public ReconciliationService(ReconciliationRunRepository runRepository,
                                 SettlementRecordRepository settlementRecordRepository,
                                 ReconciliationResultRepository resultRepository,
                                 FinancialTransactionRepository transactionRepository) {
        this.runRepository = runRepository;
        this.settlementRecordRepository = settlementRecordRepository;
        this.resultRepository = resultRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public ReconciliationRunResponse upload(MultipartFile file) {
        validateFile(file);
        String fileName = safeFileName(file.getOriginalFilename());
        ReconciliationRun run = runRepository.save(new ReconciliationRun(fileName));
        List<SettlementRecord> records = parse(file, run);
        settlementRecordRepository.saveAll(records);

        List<FinancialTransaction> transactions = transactionRepository.findAllByOrderByCreatedAtAsc();
        Map<String, FinancialTransaction> internalByReference = transactions.stream()
                .collect(Collectors.toMap(FinancialTransaction::getReferenceNumber, Function.identity()));
        Map<String, List<SettlementRecord>> externalByReference = records.stream()
                .collect(Collectors.groupingBy(SettlementRecord::getExternalReference,
                        LinkedHashMap::new, Collectors.toList()));

        List<ReconciliationResult> results = new ArrayList<>();
        for (Map.Entry<String, List<SettlementRecord>> entry : externalByReference.entrySet()) {
            String reference = entry.getKey();
            List<SettlementRecord> sameReferenceRecords = entry.getValue();
            if (sameReferenceRecords.size() > 1) {
                results.add(new ReconciliationResult(run,
                        internalByReference.containsKey(reference) ? reference : null,
                        reference, ReconciliationResultType.DUPLICATE,
                        "Settlement file contains " + sameReferenceRecords.size()
                                + " records with reference " + reference));
                continue;
            }

            SettlementRecord settlement = sameReferenceRecords.get(0);
            FinancialTransaction transaction = internalByReference.get(reference);
            if (transaction == null) {
                results.add(new ReconciliationResult(run, null, reference,
                        ReconciliationResultType.MISSING_IN_LEDGER,
                        "Settlement reference is not present in the internal ledger"));
            } else if (transaction.getAmount().compareTo(settlement.getAmount()) != 0) {
                results.add(new ReconciliationResult(run, reference, reference,
                        ReconciliationResultType.AMOUNT_MISMATCH,
                        "Internal amount " + transaction.getAmount() + " differs from settlement amount "
                                + settlement.getAmount()));
            } else if (!transaction.getStatus().name().equals(settlement.getStatus())) {
                results.add(new ReconciliationResult(run, reference, reference,
                        ReconciliationResultType.STATUS_MISMATCH,
                        "Internal status " + transaction.getStatus() + " differs from settlement status "
                                + settlement.getStatus()));
            } else {
                results.add(new ReconciliationResult(run, reference, reference,
                        ReconciliationResultType.MATCHED,
                        "Internal transaction and settlement record match"));
            }
        }

        Set<String> externalReferences = externalByReference.keySet();
        for (FinancialTransaction transaction : transactions) {
            if (!externalReferences.contains(transaction.getReferenceNumber())) {
                results.add(new ReconciliationResult(run, transaction.getReferenceNumber(), null,
                        ReconciliationResultType.MISSING_IN_SETTLEMENT,
                        "Internal transaction is not present in the settlement file"));
            }
        }

        resultRepository.saveAll(results);
        run.complete();
        return buildResponse(run, results);
    }

    @Transactional(readOnly = true)
    public ReconciliationRunResponse getRun(Long runId) {
        ReconciliationRun run = findRun(runId);
        return buildResponse(run, resultRepository.findByRun_IdOrderById(runId));
    }

    @Transactional(readOnly = true)
    public ReconciliationRunResponse getMismatches(Long runId) {
        ReconciliationRun run = findRun(runId);
        List<ReconciliationResult> mismatches = resultRepository
                .findByRun_IdAndResultTypeNotOrderById(runId, ReconciliationResultType.MATCHED);
        return buildResponse(run, mismatches);
    }

    private List<SettlementRecord> parse(MultipartFile file, ReconciliationRun run) {
        List<SettlementRecord> records = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                throw new InvalidCsvException("CSV file is empty");
            }
            List<String> columns = parseCsvLine(header);
            if (columns.size() != 3) {
                throw new InvalidCsvException("CSV header must contain reference,amount,status");
            }
            columns.set(0, columns.get(0).replace("\uFEFF", ""));
            List<String> normalizedHeader = columns.stream()
                    .map(value -> value.trim().toLowerCase(Locale.ROOT)).toList();
            if (!normalizedHeader.equals(List.of("reference", "amount", "status"))) {
                throw new InvalidCsvException("CSV header must be exactly reference,amount,status");
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                if (values.size() != 3) {
                    throw new InvalidCsvException("Line " + lineNumber + " must contain exactly three columns");
                }
                String reference = values.get(0).trim();
                String amountText = values.get(1).trim();
                String status = values.get(2).trim().toUpperCase(Locale.ROOT);
                if (reference.isBlank() || reference.length() > 50) {
                    throw new InvalidCsvException("Line " + lineNumber + " has an invalid reference");
                }

                BigDecimal amount;
                try {
                    amount = new BigDecimal(amountText).setScale(2, RoundingMode.UNNECESSARY);
                } catch (NumberFormatException | ArithmeticException exception) {
                    throw new InvalidCsvException("Line " + lineNumber + " has an invalid amount");
                }
                if (amount.signum() <= 0) {
                    throw new InvalidCsvException("Line " + lineNumber + " amount must be greater than zero");
                }
                try {
                    TransactionStatus.valueOf(status);
                } catch (IllegalArgumentException exception) {
                    throw new InvalidCsvException("Line " + lineNumber + " has an invalid status: " + status);
                }
                records.add(new SettlementRecord(run, reference, amount, status, lineNumber));
            }
        } catch (IOException exception) {
            throw new InvalidCsvException("Unable to read CSV file", exception);
        }
        if (records.isEmpty()) {
            throw new InvalidCsvException("CSV file does not contain settlement records");
        }
        return records;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        if (quoted) {
            throw new InvalidCsvException("CSV contains an unclosed quoted value");
        }
        values.add(current.toString());
        return values;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidCsvException("A non-empty CSV file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidCsvException("CSV file exceeds the 5 MB limit");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new InvalidCsvException("Uploaded file must have a .csv extension");
        }
    }

    private String safeFileName(String originalFileName) {
        String normalized = originalFileName == null ? "settlement.csv"
                : originalFileName.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String name = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        return name.length() <= 255 ? name : name.substring(name.length() - 255);
    }

    private ReconciliationRun findRun(Long runId) {
        return runRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation run not found: " + runId));
    }

    private ReconciliationRunResponse buildResponse(ReconciliationRun run,
                                                    List<ReconciliationResult> results) {
        EnumMap<ReconciliationResultType, Long> counts = new EnumMap<>(ReconciliationResultType.class);
        for (ReconciliationResultType type : ReconciliationResultType.values()) {
            counts.put(type, 0L);
        }
        results.forEach(result -> counts.merge(result.getResultType(), 1L, Long::sum));
        List<ReconciliationResultResponse> responseResults = results.stream()
                .map(result -> new ReconciliationResultResponse(
                        result.getTransactionReference(), result.getSettlementReference(),
                        result.getResultType(), result.getDescription()))
                .toList();
        return new ReconciliationRunResponse(run.getId(), run.getFileName(), run.getStatus(),
                run.getCreatedAt(), run.getCompletedAt(), results.size(), counts, responseResults);
    }
}
