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