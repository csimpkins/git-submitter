package org.cs1331.gitsubmit;

import java.util.Scanner;
import java.io.File;

public class Download {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("java Download repoSuffix owner students.txt");
            System.exit(0);
        }
        System.out.print("Username: ");
        String user = System.console().readLine();
        System.out.print("Password: ");
        String password = new String(System.console().readPassword());
        //yes the colon
        String auth = java.util.Base64.getEncoder().encodeToString((
            user + ":" + password).getBytes());

        String repoSuffix = args[0];
        String owner = args[1];
        Scanner s = new Scanner(new File(args[2]));
        while(s.hasNext()) {
            String u = s.nextLine();
            System.out.println(u);
            StudentSubmission sub = new StudentSubmission(owner, auth,
                u + repoSuffix);
            try {
                sub.download(args[3] + "/" + u);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}

