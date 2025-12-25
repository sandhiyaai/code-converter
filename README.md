# Code Converter (Python → Java)

##  Project Overview

**Code Converter** is a **rule-based source-to-source compiler** that converts **Python code into equivalent Java code**.
The project focuses on **syntax transformation**, not error detection, and is designed to work correctly for valid Python programs.

This tool is built using **Spring Boot** for the backend and a **web-based frontend** for user interaction.

---

## Features

* Converts Python code to Java
* Supports:

  * `if`, `elif`, `else`
  * `for` loops (`range`)
  * `while` loops
  * `def` functions with `return`
  * Variable assignments
  * `print()` → `System.out.println()`
* Handles:

  * `and`, `or`, `not` correctly (outside strings)
  * Nested blocks using indentation
* Rule-based conversion (no AST or ML)
* Proper Java block `{}` generation
* Error reporting for unsupported data types:

  * ❌ List
  * ❌ Dictionary
  * ❌ Tuple

---

## 🚫 Not Supported (By Design)

* Classes
* Imports
* Exception handling (`try/except`)
* Global keyword
* Python collections (list, dict, tuple)
* Error detection (syntax validation)
**These features are intentionally not supported**, as the project is a rule-based converter, not a full compiler or error detector.

> ⚠️ This compiler **assumes valid Python input** and focuses only on conversion.

---

## 🛠️ Tech Stack

### Backend

* Java
* Spring Boot
* REST API

### Frontend

* HTML
* CSS
* JavaScript

---

## 📂 Project Structure

```
code-converter/
├── backend/
│   └── CodeConvertService.java
│   └── CodeController.java
│   └── CodeTransformApplication.java
│
├── frontend/
│   ├── index.html
│   ├── style.css
│   └── script.js
│
├── README.md
```

---

##  How Conversion Works

1. **Pass 1**

   * Collects Python function definitions
   * Separates `main` code and function bodies

2. **Pass 2**

   * Converts Python syntax into Java
   * Uses indentation tracking to generate `{}` blocks
   * Resolves return types
   * Generates final Java class

---

## Testing

* Tested with **50 different Python test cases**
* Includes:

  * Nested `if-elif-else`
  * Loops inside functions
  * Logical expressions
**Accuracy Result**

**Total Test Cases:** 50

**Successfully Converted:** 50

**Accuracy:** 100% (50/50)

**Result:50/50 test cases passed**

---

## ▶️ How to Run

### Backend

```bash
mvn spring-boot:run
```

### Frontend

* Open `index.html` in browser
* Paste Python code
* Click **Convert**
* View Java output

---

## Example

### Python Input

```python
def check(x):
    if x > 5:
        return "Big"
    else:
        return "Small"

print(check(10))
```

### Java Output

```java
public class Main {
    static String check(int x) {
        if (x > 5) {
            return "Big";
        } else {
            return "Small";
        }
    }

    public static void main(String[] args) {
        System.out.println(check(10));
    }
}
```
