function greet() {
    const name = document.getElementById("name").value;

    fetch("/hello?name=" + encodeURIComponent(name))
        .then(response => response.text())
        .then(message => {
            document.getElementById("result").innerHTML = message;
        })
        .catch(error => {
            document.getElementById("result").innerHTML = "Error: " + error;
        });
}

function loadPi() {
    fetch("/pi")
        .then(response => response.text())
        .then(pi => {
            document.getElementById("pi-value").innerHTML = pi;
        })
        .catch(error => {
            document.getElementById("pi-value").innerHTML = "Error: " + error;
        });
}

document.addEventListener("DOMContentLoaded", function () {
    loadPi();
    console.log("Static JavaScript file loaded successfully.");
});
