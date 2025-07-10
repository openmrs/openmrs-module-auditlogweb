/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */

const entityInput = document.getElementById('entitySearch');
const hiddenEntityInput = document.getElementById('selectedClass');
const entityDropdown = document.getElementById('dropdownList');

function renderEntityDropdown(filter = "") {
    entityDropdown.innerHTML = "";
    let found = false;
    classes.forEach(cls => {
        if (cls.toLowerCase().includes(filter.toLowerCase())) {
            const div = document.createElement('div');
            div.className = 'dropdown-item' + (cls === currentClass ? ' selected' : '');
            div.textContent = cls;
            div.onclick = () => selectEntity(cls);
            entityDropdown.appendChild(div);
            found = true;
        }
    });
    entityDropdown.style.display = found ? "block" : "none";
    entityDropdown.classList.toggle('active', found);
}

function selectEntity(cls) {
    entityInput.value = cls;
    hiddenEntityInput.value = cls;
    entityDropdown.style.display = "none";
    entityDropdown.classList.remove('active');
    document.getElementById('auditForm').submit();
}

entityInput.addEventListener('focus', () => renderEntityDropdown(entityInput.value));
entityInput.addEventListener('click', () => renderEntityDropdown(entityInput.value));
entityInput.addEventListener('input', () => renderEntityDropdown(entityInput.value));
entityInput.removeAttribute('readonly');

if (currentClass) {
    entityInput.value = currentClass;
    hiddenEntityInput.value = currentClass;
}

const usernameInput = document.getElementById('username');
const usernameDropdown = document.getElementById('usernameDropdown');
const usernameSpinner = document.getElementById('usernameSpinner');
let usernameSuggestions = [];
let usernameActiveIndex = -1;
let usernameFetchTimeout = null;

usernameInput.addEventListener('input', () => {
    const query = usernameInput.value.trim();
    if (!query) {
        usernameDropdown.innerHTML = "";
        usernameDropdown.style.display = "none";
        return;
    }
    clearTimeout(usernameFetchTimeout);

    usernameFetchTimeout = setTimeout(() => {
        fetch(`/openmrs/module/auditlogweb/auditlogs.form/suggestUsers.form?q=${encodeURIComponent(query)}`)
            .then(response => response.json())
            .then(data => {
                usernameSuggestions = data || [];
                renderUsernameDropdown();
            })
            .catch(err => {
                console.error("Failed to fetch usernames", err);
                usernameSuggestions = [];
                renderUsernameDropdown();
            })
    }, 300); // debounce
});

function renderUsernameDropdown() {
    usernameDropdown.innerHTML = "";
    usernameActiveIndex = -1;
    if (!usernameSuggestions.length) {
        usernameDropdown.style.display = "none";
        return;
    }

    usernameSuggestions.forEach((username, index) => {
        const div = document.createElement('div');
        div.className = 'dropdown-item';
        div.textContent = username;
        div.addEventListener('click', () => {
            usernameInput.value = username;
            usernameDropdown.style.display = "none";
        });
        usernameDropdown.appendChild(div);
    });

    usernameDropdown.style.display = "block";
}

usernameInput.addEventListener('keydown', (e) => {
    const items = usernameDropdown.querySelectorAll('.dropdown-item');
    if (!items.length) return;

    switch (e.key) {
        case 'ArrowDown':
            usernameActiveIndex = (usernameActiveIndex + 1) % items.length;
            updateActiveItem(items);
            e.preventDefault();
            break;
        case 'ArrowUp':
            usernameActiveIndex = (usernameActiveIndex - 1 + items.length) % items.length;
            updateActiveItem(items);
            e.preventDefault();
            break;
        case 'Enter':
            if (usernameActiveIndex >= 0) {
                usernameInput.value = items[usernameActiveIndex].textContent;
                usernameDropdown.style.display = "none";
                usernameDropdown.innerHTML = "";
            }
            break;
    }
});

function updateActiveItem(items) {
    items.forEach((el, i) => {
        el.classList.toggle('selected', i === usernameActiveIndex);
        if (i === usernameActiveIndex) {
            el.scrollIntoView({ block: 'nearest' });
        }
    });
}

document.addEventListener('click', (e) => {
    if (!entityInput.contains(e.target) && !entityDropdown.contains(e.target)) {
        entityDropdown.style.display = "none";
        entityDropdown.classList.remove('active');
    }

    if (!usernameInput.contains(e.target) && !usernameDropdown.contains(e.target)) {
        usernameDropdown.style.display = "none";
        usernameDropdown.innerHTML = "";
    }
});

function goToPage(page) {
    document.getElementById('pageInput').value = page;
    document.getElementById('auditForm').submit();
}