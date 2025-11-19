import javax.swing.*;
import java.awt.Color;
import helper_classes.*;

public class WindowBuilder {
  public static void main(String[] args) {

     JFrame frame = new JFrame("My Awesome Window");
     frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
     frame.setSize(768, 415);
     JPanel panel = new JPanel();
     panel.setLayout(null);
     panel.setBackground(Color.decode("#1e1e1e"));

     JButton element1 = new JButton("Click Me");
     element1.setBounds(340, 312, 106, 28);
     element1.setBackground(Color.decode("#2e2e2e"));
     element1.setForeground(Color.decode("#D9D9D9"));
     element1.setFont(CustomFontLoader.loadFont("./resources/fonts/Lato.ttf", 14));
     element1.setBorder(new RoundedBorder(4, Color.decode("#979797"), 1));
     element1.setFocusPainted(false);
     OnClickEventHelper.setOnClickColor(element1, Color.decode("#232323"), Color.decode("#2e2e2e"));
     panel.add(element1);

     JTextArea element2 = new JTextArea("");
     element2.setBounds(293, 72, 374, 39);
     element2.setFont(CustomFontLoader.loadFont("./resources/fonts/Lato.ttf", 14));
     element2.setBackground(Color.decode("#B2B2B2"));
     element2.setForeground(Color.decode("#656565"));
     element2.setBorder(new RoundedBorder(2, Color.decode("#979797"), 0));
     OnFocusEventHelper.setOnFocusText(element2, "Your long Input!", Color.decode("#353535"),   Color.decode("#656565"));
     panel.add(element2);

     JLabel element3 = new JLabel("PATH:");
     element3.setBounds(195, 84, 106, 26);
     element3.setFont(CustomFontLoader.loadFont("./resources/fonts/Lato.ttf", 24));
     element3.setForeground(Color.decode("#D9D9D9"));
     panel.add(element3);

     JTextArea element5 = new JTextArea("");
     element5.setBounds(293, 126, 374, 39);
     element5.setFont(CustomFontLoader.loadFont("./resources/fonts/Lato.ttf", 14));
     element5.setBackground(Color.decode("#B2B2B2"));
     element5.setForeground(Color.decode("#656565"));
     element5.setBorder(new RoundedBorder(2, Color.decode("#979797"), 0));
     OnFocusEventHelper.setOnFocusText(element5, "Your long Input!", Color.decode("#353535"),   Color.decode("#656565"));
     panel.add(element5);

     JButton element6 = new JButton("Click Me");
     element6.setBounds(679, 74, 34, 35);
     element6.setBackground(Color.decode("#2e2e2e"));
     element6.setForeground(Color.decode("#D9D9D9"));
     element6.setFont(CustomFontLoader.loadFont("./resources/fonts/Lato.ttf", 14));
     element6.setBorder(new RoundedBorder(4, Color.decode("#979797"), 1));
     element6.setFocusPainted(false);
     OnClickEventHelper.setOnClickColor(element6, Color.decode("#232323"), Color.decode("#2e2e2e"));
     panel.add(element6);

     JButton element7 = new JButton("Click Me");
     element7.setBounds(674, 130, 35, 35);
     element7.setBackground(Color.decode("#2e2e2e"));
     element7.setForeground(Color.decode("#D9D9D9"));
     element7.setFont(CustomFontLoader.loadFont("./resources/fonts/Lato.ttf", 14));
     element7.setBorder(new RoundedBorder(4, Color.decode("#979797"), 1));
     element7.setFocusPainted(false);
     OnClickEventHelper.setOnClickColor(element7, Color.decode("#232323"), Color.decode("#2e2e2e"));
     panel.add(element7);

     JLabel element8 = new JLabel("OUTPUT:");
     element8.setBounds(161, 137, 102, 26);
     element8.setFont(CustomFontLoader.loadFont("./resources/fonts/Lato.ttf", 24));
     element8.setForeground(Color.decode("#D9D9D9"));
     panel.add(element8);

     frame.add(panel);
     frame.setVisible(true);

  }
}