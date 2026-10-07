function itemFilter() {
    // Declare variables
    var input, filter, ul, li, a, i, txtValue;
    input = document.getElementById('searchInput');
    filter = input.value.toUpperCase();
    ul = document.getElementById("searchUL");
    li = ul.getElementsByTagName('li');

    // Loop through all list items, and hide those who don't match the search query
    for (i = 0; i < li.length; i++) {
        a = li[i].getElementsByTagName("span")[0];
        txtValue = a.textContent || a.innerText;
        if (txtValue.toUpperCase().indexOf(filter) > -1) {
            li[i].style.display = "";
        } else {
            li[i].style.display = "none";
        }
    }
}

//for the input with clicking
function changeInput(evt, id) {
    if (document.getElementById('I' + id).getAttribute('checked') === 'false' || document.getElementById('I' + id).getAttribute('checked') === null) {
        document.getElementById('I' + id).setAttribute('checked', 'true');
        evt.currentTarget.className += " active ";
        sprites.push(id);
    } else {
        document.getElementById('I' + id).removeAttribute('checked');
        evt.currentTarget.className = evt.currentTarget.className.replace('active', " ");
        sprites = sprites.filter(e => e !== id);
    }
}

//for the input where you type
function changeInputKey(evt, id) {
    if (document.getElementById('firstRatio').value > 0 && document.getElementById('secondRatio').value > 0) {
        if (!evt.currentTarget.parentElement.className.includes('active')) {
            evt.currentTarget.parentElement.className += " active";
            document.getElementById('Ijunction_ratio').setAttribute('checked', 'true');
        }
    } else {
        evt.currentTarget.parentElement.className = evt.currentTarget.parentElement.className.replace('active', " ");
        document.getElementById('Ijunction_ratio').removeAttribute('checked');
    }
}

let enchantments = 0;

function changeInputButton(evt, id) {
    if (document.getElementById(id).checked) {
        enchantments--;
    } else {
        enchantments++;
    }

    if (enchantments == 0) {
        evt.currentTarget.parentElement.className = evt.currentTarget.parentElement.className.replace('active', '');
    } else {
        evt.currentTarget.parentElement.className += ' active';
    }
}