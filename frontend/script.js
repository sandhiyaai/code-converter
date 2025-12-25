const inputArea = document.getElementById("inputCode");
const lineNumbers = document.getElementById("lineNumbers");
const convertBtn = document.getElementById("convertBtn");

/* ===== CONVERT ===== */
function convertCode() {
    const code = inputArea.value;

    fetch("http://localhost:8080/api/convert", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            code: code,
            sourceLanguage: "python",
            targetLanguage: "java"
        })
    })
    .then(res => res.json())
    .then(data => {
        document.getElementById("outputCode").value = data.output;
    })
    .catch(() => alert("Backend not connected"));
}

/* ===== AUTO INDENT (PYTHON) ===== */
inputArea.addEventListener("keydown", function (e) {

    // ENTER → auto indent
    if (e.key === "Enter") {
        e.preventDefault();

        const start = this.selectionStart;
        const before = this.value.substring(0, start);
        const after = this.value.substring(start);

        const lines = before.split("\n");
        const currentLine = lines[lines.length - 1];

        let indent = currentLine.match(/^(\s*)/)[1];

        if (currentLine.trim().endsWith(":")) {
            indent += "    ";
        }

        this.value = before + "\n" + indent + after;
        this.selectionStart = this.selectionEnd =
            before.length + 1 + indent.length;
    }

    // TAB → 4 spaces
    if (e.key === "Tab") {
        e.preventDefault();
        const start = this.selectionStart;
        this.value =
            this.value.substring(0, start) +
            "    " +
            this.value.substring(start);
        this.selectionStart = this.selectionEnd = start + 4;
    }
});

/* ===== LINE NUMBERS ===== */
function updateLineNumbers() {
    const lines = inputArea.value.split("\n").length || 1;
    lineNumbers.innerHTML = Array.from(
        { length: lines },
        (_, i) => i + 1
    ).join("<br>");
}
inputArea.addEventListener("input", () => {
    updateLineNumbers();
    convertBtn.disabled = inputArea.value.trim().length === 0;
});

inputArea.addEventListener("scroll", () => {
    lineNumbers.scrollTop = inputArea.scrollTop;
});



updateLineNumbers();










