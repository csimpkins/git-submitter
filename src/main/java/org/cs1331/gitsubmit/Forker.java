package org.cs1331.gitsubmit;

import java.util.Scanner;
import java.io.File;
import javax.swing.JPasswordField;
import javax.swing.JOptionPane;

public class Forker {
    private static Scanner scan = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args[0].contains("-h")) {
            System.out.println("Usage:");
            System.out.println("java Forker repositorySuffix [organization] studentsFile");
            System.out.println("e.g:");
            System.out.println("java Forker -timedlab cs1331-timedlabs b1.txt");
            System.exit(0);
        }
        String user = "";
        String password = "";
        String auth = "";
        do {
            if (!user.equals("")) {
                System.err.println("Login failed, try again: ");
            }
            System.err.print("Username: ");
            user = scan.nextLine();
            System.err.println("Password Popup Displayed");
            JPasswordField pf = new JPasswordField();
            int res = JOptionPane.showConfirmDialog(null, pf, "Password:",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res == JOptionPane.OK_OPTION) {
                password = new String(pf.getPassword());
            } else {
                System.exit(0);
            }
            //yes the colon
            auth = java.util.Base64.getEncoder().encodeToString((
                user + ":" + password).getBytes());
        } while (!StudentSubmission.testAuth(auth));

        String repoSuffix = args[0];
        String org = args[1];
        Scanner s = new Scanner(new File(args[args.length > 2 ? 2 : 1]));
        boolean delete = false;
        for (String s2 : args) {
            if (s2.equals("-d")) delete = true;
        }
        while (s.hasNext()) {
            String u = s.nextLine();
            if (delete) {
                StudentSubmission ss =
                    new StudentSubmission(user, auth, u + repoSuffix);
                System.out.println(u);
                System.out.println(ss.delete());
            } else {
                StudentSubmission ss =
                    new StudentSubmission(u, auth, u + repoSuffix);
                System.out.println(u);
                System.out.println(args.length > 2 ? ss.fork(org) : ss.fork());
                new StudentSubmission(
                    args.length > 2 ? org : user, auth, u + repoSuffix)
                    .removeCollab(u);
            }
        }
    }
}
