
const input = document.getElementById('entitySearch');
const hiddenInput = document.getElementById('selectedClass');
const dropdown = document.getElementById('dropdownList');

function renderDropdown(filter = "") {
    dropdown.innerHTML = "";
    let found = false;
    classes.forEach(cls => {
        if (cls.toLowerCase().includes(filter.toLowerCase())) {
            const div = document.createElement('div');
            div.className = 'dropdown-item' + (cls === currentClass ? ' selected' : '');
            div.textContent = cls;
            div.onclick = function() {
                input.value = cls;
                hiddenInput.value = cls;
                dropdown.style.display = "none";
                dropdown.classList.remove('active');
                document.getElementById('auditForm').submit();
            };
            dropdown.appendChild(div);
            found = true;
        }
    });
    dropdown.style.display = found ? "block" : "none";
    dropdown.classList.toggle('active', found);
}

input.addEventListener('focus', () => renderDropdown(input.value));
input.addEventListener('click', () => renderDropdown(input.value));
input.removeAttribute('readonly');
input.addEventListener('input', () => renderDropdown(input.value));

document.addEventListener('click', function(e) {
    if (!input.contains(e.target) && !dropdown.contains(e.target)) {
        dropdown.style.display = "none";
        dropdown.classList.remove('active');
    }
});

if (currentClass) {
    input.value = currentClass;
    hiddenInput.value = currentClass;
}