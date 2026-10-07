let sprites = [];


function drawBoard(canvas) {
    //grid square width
    let context = canvas.getContext("2d");
    const cw = 32;	//cell width
    const ch = 32;	//cell height
    const bw = canvas.width;
    const bh = canvas.height;


    for (let x = 0; x <= bw; x += cw) {
        context.moveTo(0.5 + x, 0);
        context.lineTo(0.5 + x, bh);
    }

    //grid square height
    for (let x = 0; x <= bh; x += ch) {
        context.moveTo(0, 0.5 + x);
        context.lineTo(bw, 0.5 + x);
    }
    context.strokeStyle = "black";
    context.stroke();
}

function filterSprites(state) {
    const ul = document.getElementById("searchUL");
    for (const item of ul.children) {
        if (item.children[0].className.includes("active") != state.checked) {
            item.style.display = 'none'
        } else {
            item.style.display = ''
        }
    }
}

//so this is going to dynamically add the tiles to the table
function loadItems() {
    //this is the table we are using
    const table = document.getElementById("items").children[0];
    table.removeChild(table.children[0]);
    table.appendChild(document.createElement("tbody"));
    let row = table.insertRow(0);
    let column = 0;
    const c = row.insertCell(column);
    const smallC = document.createElement("canvas");
    smallC.width = 32;
    smallC.height = 32;
    c.setAttribute("id", 1000);
    c.onclick = function () { //insert whatever function handled the tiles selection here}
        editor.selection = parseInt(this.id);
    }
    c.appendChild(smallC);
    column++;
    for (let sprite of sprites) {

        //adding a new row
        if (column == 0) {
            row = table.insertRow(table.rows.length);
        }
        //adding a new cell at the index of the column
        const cell = row.insertCell(column);
        cell.setAttribute("id", sprite);
        cell.onclick = function () { //insert whatever function handled the tiles selection here}
            editor.selection = sprite;
        }
        //setting the image, we need to create different icons for every image it seems, every cell has his own id as well to use with the function,
        //this should all work
        const smallC = document.createElement("canvas");
        smallC.width = 32;
        smallC.height = 32;
        smallC.getContext('2d').drawImage(
            document.getElementById(`IM${sprite}`), //the image
            0, //start x of the image in the spritesheet
            0, //start y of the image in the spritesheet
            16, //width of the sprite
            16, //height of the sprite
            0, //destination x of the sprite on the canvas
            0, //destination of y of the sprite on the canvas
            32, //width of sprite
            32 //height of sprite
        );
        cell.appendChild(smallC);
        column++;
        if (column == 3) { column = 0; }
    }
}