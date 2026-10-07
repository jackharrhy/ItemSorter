function fixImgSrc() {
    var woods = ["oak", "spruce", "acacia", "dark_oak", "jungle", "birch"];
    var elem = document.getElementsByTagName('img');
    for (var i = 0; i < elem.length; i++) {
        var e = elem[i];
        var src = e.getAttribute("src");

        //fixing the slabs
        if (src.includes('slab')) {
            src = src.replace("_slab", "");


            if (woods.includes(src.split("/")[2].split(".")[0])) {
                src = src.replace(src.split("/")[2].split(".")[0], src.split("/")[2].split(".")[0] + "_planks");
            }
            if (src.includes("quartz")) {
                src = src.replace("quartz", "quartz_block");
            }
            if (src.includes("brick.")) {
                src = src.replace("brick", "bricks");
            }
            if (src.includes("smooth") && !src.includes("_stone")) {
                src = src.replace("smooth_", "");
            }
            if (src.includes("purpur")) {
                src = src.replace("purpur", "purpur_block");
            }
            e.setAttribute("src", src);


            e.style.clipPath = "polygon(0% 0%, 100% 0%, 100% 50%, 0% 50%)";
            e.style.position = "relative";
            e.style.transform = "translateY(8px)";

            //fixing the stairs
        } else if (src.includes('stairs')) {
            src = src.replace("_stairs", "");

            if (woods.includes(src.split("/")[2].split(".")[0])) {
                src = src.replace(src.split("/")[2].split(".")[0], src.split("/")[2].split(".")[0] + "_planks");
            }
            if (src.includes("quartz")) {
                src = src.replace("quartz", "quartz_block");
            }
            if (src.includes("brick.")) {
                src = src.replace("brick", "bricks");
            }
            if (src.includes("smooth") && !src.includes("_stone")) {
                src = src.replace("smooth_", "");
            }
            if (src.includes("purpur")) {
                src = src.replace("purpur", "purpur_block");
            }

            e.setAttribute("src", src);
            e.style.clipPath = "polygon(0% 0%, 50% 0%, 50% 50%, 100% 50%, 100% 100%, 0% 100%)";
            e.style.position = "relative";
            e.style.transform = "translateY(8px)";

            //fixing the pressure plates
        } else if (src.includes("pressure_plate")) {
            src = src.replace("_pressure_plate", "");
            if (woods.includes(src.split("/")[2].split(".")[0])) {
                src = src.replace(src.split("/")[2].split(".")[0], src.split("/")[2].split(".")[0] + "_planks");
            } else if (src.includes("heavy_weighted")) {
                src = src.replace("heavy_weighted", "iron_block");
            } else if (src.includes("light_weighted")) {
                src = src.replace("light_weighted", "gold_block");
            }

            e.style.clipPath = "polygon(10% 10%, 90% 10%, 90% 90%, 10% 90%)";
            e.style.position = "relative";
            e.style.transform = "translateY(2px)";

            e.setAttribute("src", src);
        } else if (src.includes("button")) {
            src = src.replace("_button", "");

            if (woods.includes(src.split("/")[2].split(".")[0])) {
                src = src.replace(src.split("/")[2].split(".")[0], src.split("/")[2].split(".")[0] + "_planks");
            }

            e.style.clipPath = "polygon(20% 30%, 80% 30%, 80% 60%, 20% 60%)";
            e.style.position = "relative";
            e.style.transform = "translateY(4px)";
            e.setAttribute("src", src);
        } else if (src.includes("carpet")) {
            src = src.replace("_carpet", "_wool");
            e.style.clipPath = "polygon(10% 10%, 90% 10%, 90% 90%, 10% 90%)";
            e.style.position = "relative";
            e.style.transform = "translateY(2px)";

            e.setAttribute("src", src);
        } else if (src.includes('stripped')) {
            e.setAttribute("src", src.replace("_wood", "_log"));
        } else if (src.includes('_wood')) {
            e.setAttribute("src", src.replace("_wood", "_log"));
        } else if (src.includes("smooth") && !src.includes("_stone")) {
            src = src.replace("smooth_", "");

            if (src.includes("quartz")) {
                src = src.replace("quartz", "quartz_block");
            }
            e.setAttribute("src", src);
        } else if (src.includes("snow_block")) {
            e.setAttribute("src", src.replace("_block", ""));
        } else if (src.includes("infested")) {
            e.setAttribute("src", src.replace("infested_", ""));
        } else if (src.includes("pane")) {
            e.setAttribute("src", src.replace("_pane", ""));
        } else if (src.includes("enchanted_golden")) {
            e.setAttribute("src", src.replace("enchanted_", ""));
        }
    }
}