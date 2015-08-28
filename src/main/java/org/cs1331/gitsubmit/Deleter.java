package org.cs1331.gitsubmit;

public class Deleter {
    private static int delete(String repo, String auth) throws Exception {
        return StudentSubmission.doRequest(String.format("repos/cs1331/cs1331-tl0-feb03%s", repo),
            "DELETE", auth, "", null, null, null);
    }

    public static void main(String[] args) throws Exception {
        String auth = java.util.Base64.getEncoder().encodeToString(("tshields3:"
            + new String(System.console().readPassword())).getBytes());

        for (int i = 18; i < 40; i++) {
            for (int j = 0; j < 10; j++) {
                System.out.println(delete(i + "-" + j, auth));
            }
        }
    }
    }
 
