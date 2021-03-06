package gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;

public class FrameDetailCellsRenderer extends JLabel implements TreeCellRenderer {

	public FrameDetailCellsRenderer(){
		this.setOpaque(true);
	}
	
	public Component getTreeCellRendererComponent(JTree tree, Object value,
													boolean isSelected, boolean isExpanded, boolean isLeaf, 
													int row, boolean hasFocus) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		
		this.setBackground(tree.getBackground());
		this.setForeground(tree.getForeground());

		String[] val = node.getUserObject().toString().split(","); 
		if(val[0].equals("STXE")){
			this.setBackground(Color.BLACK);
			this.setForeground(Color.WHITE);
			this.setText("BAD STX");
		} else if(val[0].equals("ETXE")){
			this.setBackground(Color.BLACK);
			this.setForeground(Color.WHITE);
			this.setText("BAD STX");
		} else if(val[0].equals("CRCE")){
			this.setBackground(Color.BLACK);
			this.setForeground(Color.WHITE);
			this.setText("BAD CRC : \n got "+ val[1] +" expected "+ val[2]);
		} else if(val[0].equals("HeaderE")){
			this.setBackground(Color.red);
			this.setForeground(Color.white);
			this.setText("Header");
		} else if(val[0].equals("FooterE")){
			this.setBackground(Color.red);
			this.setForeground(Color.white);
			this.setText("Footer");
		} else {
			this.setText(val[0]);
		}
		
		return this;
	}

}
