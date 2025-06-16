/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
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