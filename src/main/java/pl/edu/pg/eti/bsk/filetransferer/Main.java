package pl.edu.pg.eti.bsk.filetransferer;

import pl.edu.pg.eti.bsk.filetransferer.ui.Gui;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            Gui gui = new Gui(false);
        } else if (args[0].equals("ttm")){
            Gui gui = new Gui(true);
        }
    }

}
