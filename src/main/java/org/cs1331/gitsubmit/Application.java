package org.cs1331.gitsubmit;

import java.util.List;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;
import java.util.stream.Collectors;
import javax.swing.JPasswordField;
import javax.swing.JOptionPane;
import java.io.File;
import java.awt.Desktop;
import java.net.URI;
import com.google.gson.Gson;

/* Exceptions*/
import java.net.UnknownHostException;

public class Application {

    private static final String DEFAULT_CONFIG = "config.json";

    private static boolean checkCompile(String[] files) throws Exception {
        String combined = Arrays.stream(files).collect(Collectors.joining(" "));
        List<String> args = Arrays.stream(combined.split(" ")).filter(f -> f
            .substring(f.lastIndexOf('.')).equals(".java"))
            .collect(Collectors.toList());
        if (args.size() == 0) return true;
        args.add(0, "javac");
        ProcessBuilder pcb = new ProcessBuilder(args);
        Process p = pcb.start();
        p.waitFor();
        return p.exitValue() == 0;
    }

    private static StudentSubmission authenticateAndCreate(String repo)
        throws Exception {
        String user = "", password = "", base64 = "";
        String lines = "------------------------------------------";
        System.out.println();
        System.out.println(lines);
        System.out.println("    Logging in to Georgia Tech servers    ");
        System.out.println(lines);

        boolean success = false;
        boolean twoFactor = false;
        String code = null;
        do {
            if (base64.length() > 0) {
                System.out.println(lines);
                System.out.println("      Login failed, please try again");
                System.out.println(lines);
            }
            user = JOptionPane.showInputDialog(null,
                "Username (e.g, gburdell3):");
            JPasswordField pf = new JPasswordField();
            int res = JOptionPane.showConfirmDialog(null, pf, "Password:",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res == JOptionPane.OK_OPTION) {
                password = new String(pf.getPassword());
            } else {
                System.exit(0);
            }

            /*System.out.print("\tUsername: ");
            user = System.console().readLine();
            System.out.print("\tPassword: ");
            password = new String(System.console().readPassword());
                */
            base64 = Base64.getEncoder().encodeToString((user + ":" + password)
                .getBytes());

            try {
                if (!twoFactor)
                    success = StudentSubmission.testAuth(base64);
            } catch (TwoFactorAuthException ex) {
                twoFactor = true;
                System.out.println("two factor exception");
            }

            if (twoFactor) {
                code = JOptionPane.showInputDialog(null, "Two-Factor Code:");
                success = StudentSubmission.testTwoFactorAuth(base64, code);
            }
        } while (!success);
        System.out.println("------------------------------------------");
        System.out.println("            Authenticated!");
        System.out.println("------------------------------------------");
        return new StudentSubmission(user, base64, user + repo, code);
    }

    private static Config processConfig(String file) throws Exception {
        return new Gson().fromJson(new Scanner(new File(file))
            .useDelimiter("\\A").next(), Config.class);
    }

    private static class Config {
        public String[] collaborators;
        public String commitMsg;
        public String repoSuffix;
    }

    public static void main(String... args) throws Exception {
        try {
            Config config = processConfig(args[0]);

            System.out.print("Compiling files.....");
            if (!checkCompile(args)) {
                System.out.println("\nYour submission does not compile. "
                    + "Please fix your compile errors before proceeding.");
                System.exit(1);
            }
            System.out.println("...done");

            StudentSubmission sub = authenticateAndCreate(config.repoSuffix);
            sub.createRepo();
            //TODO checkstyle option
            //TODO checksum/signature inside zip


            //TODO: move this into forker, just push multiple files
            //String[] fileNames = args; //change
            /*
            System.out.print("Zipping files.......");
            File submissionZip = Utils.zipFiles(config.submissionPrefix
                + sub.getUser() + ".zip",
                Arrays.stream(fileNames).map(File::new).toArray(File[]::new));
            System.out.println("...done");
            */
            for (String s : config.collaborators) sub.addCollab(s);

            String fileString = "";
            for (int i = 1; i < args.length; i++) {
                fileString += args[i] + " ";
            }

            String[] files = fileString.replaceAll("'", "").split(" ");
            boolean success =
                Arrays.stream(files).map(String::trim).filter(f -> f
                    .substring(f.lastIndexOf('.'))
                    .equals(".java")).map((String s) -> {
                        try {
                            return sub.pushFile(new File(s), config.commitMsg);
                        } catch (Exception e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                }).reduce(true, (a,b) -> a && b);
            if (success) {
                System.out.println("Code submitted successfully!");
                System.out.println("Launching browser to view repo...");
                Desktop.getDesktop().browse(new URI(
                    String.format("http://github.gatech.edu/%s/%s", sub.getUser(),
                    sub.getRepo())));
            }
            else
                System.out.println("Submission unsuccessful. :(");
        } catch (UnknownHostException ex) {
            System.out.println("Internet connection failed. Please repair your"
                + " connection and try again.");
        } catch (Exception e) {
            System.out.println("Something went wrong. Please check your"
                + " internet connection and that the source files are present."
                + " Call a TA for help if you need further assistance.");

            System.out.println(e.getMessage());
        }
    }

}
