package pl.edu.pg.eti.bsk.filetransferer;

import pl.edu.pg.eti.bsk.filetransferer.data.Server;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        int port0 = Integer.parseInt(args[0]);
        String ip = args[1];
        int port = Integer.parseInt(args[2]);

        MessageCli cli = new MessageCli(
          port0, ip, port
        );
        cli.startServer();
        System.out.println("press any key to connect");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        cli.connect();
    }

}
