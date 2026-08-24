// ==================================================
// BASE API URL
// ==================================================

const API_BASE_URL = "";


// ==================================================
// GET ELEMENT
// ==================================================

function getElement(id) {
    return document.getElementById(id);
}


// ==================================================
// SHOW SUCCESS MESSAGE
// ==================================================

function showSuccess(message) {

    const messageBox = getElement("message");

    messageBox.innerText = message;

    messageBox.className = "success-message";

    setTimeout(() => {
        messageBox.className = "";
        messageBox.innerText = "";
    }, 5000);
}


// ==================================================
// SHOW ERROR MESSAGE
// ==================================================

function showError(message) {

    const messageBox = getElement("message");

    messageBox.innerText = message;

    messageBox.className = "error-message";

    setTimeout(() => {
        messageBox.className = "";
        messageBox.innerText = "";
    }, 5000);
}


// ==================================================
// SWITCH BETWEEN SECTIONS
// ==================================================

function showSection(sectionId, button) {

    const sections =
        document.querySelectorAll(".section");

    sections.forEach(section => {
        section.classList.remove("active-section");
    });


    getElement(sectionId)
        .classList.add("active-section");


    const buttons =
        document.querySelectorAll(".nav-btn");

    buttons.forEach(btn => {
        btn.classList.remove("active");
    });


    button.classList.add("active");
}


// ==================================================
// GENERIC API REQUEST FUNCTION
// ==================================================

async function apiRequest(url, options = {}) {

    const response =
        await fetch(API_BASE_URL + url, options);

    const text =
        await response.text();


    let data;

    try {

        data = text
            ? JSON.parse(text)
            : {};

    } catch {

        data = {
            response: text
        };

    }


    if (!response.ok) {

        throw new Error(
            data.message ||
            data.error ||
            "Request failed with status " +
            response.status
        );

    }


    return data;
}


// ==================================================
// CREATE ACCOUNT
// ==================================================

async function createAccount() {

    try {

        const accountNumber =
            getElement("accountNumber").value.trim();

        const customerName =
            getElement("customerName").value.trim();

        const initialBalance =
            Number(
                getElement("initialBalance").value
            );


        const requestBody = {

            accountNumber: accountNumber,

            customerName: customerName,

            initialBalance: initialBalance

        };


        const data =
            await apiRequest(
                "/api/accounts",
                {
                    method: "POST",

                    headers: {
                        "Content-Type":
                            "application/json"
                    },

                    body:
                        JSON.stringify(requestBody)
                }
            );


        getElement("accountResult").innerText =
            JSON.stringify(data, null, 2);


        showSuccess(
            "Account created successfully!"
        );

    } catch (error) {

        showError(error.message);

    }

}


// ==================================================
// GET ACCOUNT
// ==================================================

async function getAccount() {

    try {

        const accountNumber =
            getElement("getAccountNumber")
                .value
                .trim();


        const data =
            await apiRequest(
                "/api/accounts/" +
                encodeURIComponent(accountNumber)
            );


        getElement("accountResult").innerText =
            JSON.stringify(data, null, 2);


        showSuccess(
            "Account retrieved successfully!"
        );

    } catch (error) {

        showError(error.message);

    }

}


// ==================================================
// GET ACCOUNT STATEMENT
// ==================================================

async function getStatement() {

    try {

        const accountNumber =
            getElement("statementAccountNumber")
                .value
                .trim();


        const data =
            await apiRequest(
                "/api/accounts/" +
                encodeURIComponent(accountNumber) +
                "/statement"
            );


        getElement("statementResult").innerText =
            JSON.stringify(data, null, 2);


        showSuccess(
            "Statement retrieved successfully!"
        );

    } catch (error) {

        showError(error.message);

    }

}


// ==================================================
// CREATE TRANSFER
// ==================================================

async function createTransfer() {

    try {

        const sourceAccount =
            getElement("sourceAccount")
                .value
                .trim();

        const destinationAccount =
            getElement("destinationAccount")
                .value
                .trim();

        const amount =
            Number(
                getElement("transferAmount").value
            );

        const idempotencyKey =
            getElement("idempotencyKey")
                .value
                .trim();


        const requestBody = {

            sourceAccount: sourceAccount,

            destinationAccount:
                destinationAccount,

            amount: amount,

            idempotencyKey:
                idempotencyKey

        };


        const data =
            await apiRequest(
                "/api/transfers",
                {
                    method: "POST",

                    headers: {
                        "Content-Type":
                            "application/json"
                    },

                    body:
                        JSON.stringify(requestBody)
                }
            );


        getElement("transferResult").innerText =
            JSON.stringify(data, null, 2);


        showSuccess(
            "Transfer completed successfully!"
        );

    } catch (error) {

        showError(error.message);

    }

}


// ==================================================
// GET TRANSFER
// ==================================================

async function getTransfer() {

    try {

        const referenceNumber =
            getElement("referenceNumber")
                .value
                .trim();


        const data =
            await apiRequest(
                "/api/transfers/" +
                encodeURIComponent(referenceNumber)
            );


        getElement("transferLookupResult").innerText =
            JSON.stringify(data, null, 2);


        showSuccess(
            "Transfer retrieved successfully!"
        );

    } catch (error) {

        showError(error.message);

    }

}


// ==================================================
// REVERSE TRANSFER
// ==================================================

async function reverseTransfer() {

    try {

        const referenceNumber =
            getElement("referenceNumber")
                .value
                .trim();


        const data =
            await apiRequest(
                "/api/transfers/" +
                encodeURIComponent(referenceNumber) +
                "/reverse",
                {
                    method: "POST"
                }
            );


        getElement("transferLookupResult").innerText =
            JSON.stringify(data, null, 2);


        showSuccess(
            "Transfer reversed successfully!"
        );

    } catch (error) {

        showError(error.message);

    }

}


// ==================================================
// UPLOAD SETTLEMENT CSV
// ==================================================

async function uploadCsv() {

    try {

        const fileInput =
            getElement("csvFile");


        const file =
            fileInput.files[0];


        if (!file) {

            showError(
                "Please select a CSV file."
            );

            return;

        }


        const formData =
            new FormData();


        formData.append(
            "file",
            file
        );


        const data =
            await apiRequest(
                "/api/reconciliation/upload",
                {
                    method: "POST",

                    body: formData
                }
            );


        getElement("uploadResult").innerText =
            JSON.stringify(data, null, 2);


        showSuccess(
            "Settlement file uploaded successfully!"
        );

    } catch (error) {

        showError(error.message);

    }

}


// ==================================================
// GET RECONCILIATION SUMMARY
// ==================================================

async function getReconciliation() {

    try {

        const runId =
            getElement("runId")
                .value
                .trim();


        const data =
            await apiRequest(
                "/api/reconciliation/" +
                encodeURIComponent(runId)
            );


        getElement("reconciliationResult").innerText =
            JSON.stringify(data, null, 2);


        showSuccess(
            "Reconciliation summary retrieved!"
        );

    } catch (error) {

        showError(error.message);

    }

}


// ==================================================
// GET RECONCILIATION MISMATCHES
// ==================================================

async function getMismatches() {

    try {

        const runId =
            getElement("runId")
                .value
                .trim();


        const data =
            await apiRequest(
                "/api/reconciliation/" +
                encodeURIComponent(runId) +
                "/mismatches"
            );


        getElement("reconciliationResult").innerText =
            JSON.stringify(data, null, 2);


        showSuccess(
            "Mismatches retrieved successfully!"
        );

    } catch (error) {

        showError(error.message);

    }

}


// ==================================================
// RUN INTEGRITY CHECK
// ==================================================

async function runIntegrityCheck() {

    try {

        const data =
            await apiRequest(
                "/api/integrity-check",
                {
                    method: "POST"
                }
            );


        getElement("integrityResult").innerText =
            JSON.stringify(data, null, 2);


        showSuccess(
            "Integrity check completed!"
        );

    } catch (error) {

        showError(error.message);

    }

}


// ==================================================
// CHECK BACKEND STATUS
// ==================================================

async function checkBackend() {

    try {

        const response =
            await fetch("/v3/api-docs");


        if (response.ok) {

            getElement("serverStatus").innerText =
                "● Backend Connected";

        } else {

            getElement("serverStatus").innerText =
                "● Backend Unavailable";

        }

    } catch (error) {

        getElement("serverStatus").innerText =
            "● Start Spring Boot Backend";

    }

}


// RUN WHEN PAGE LOADS

checkBackend();