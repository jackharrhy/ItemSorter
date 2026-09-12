package tollenaar.stephen.ItemSorter.Util.Web;

import java.awt.image.BufferedImage;

import tollenaar.stephen.ItemSorter.Util.Text;

public final class Image {
	
	private String src;
	private String name;
	private BufferedImage img;
	
	public Image(String src, BufferedImage img) {
		this.setSrc(src);
		this.setName(Text.capitalizeFully(src.toLowerCase().replace("_", " ").split("/")[2].replace(".png", "")));
		this.setImg(img);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSrc() {
		return src;
	}

	public void setSrc(String src) {
		this.src = src;
	}

	public BufferedImage getImg() {
		return img;
	}

	public void setImg(BufferedImage img) {
		this.img = img;
	}

}
