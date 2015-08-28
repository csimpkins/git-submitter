package org.cs1331.gitsubmit;

import java.net.URL;
import java.net.MalformedURLException;

import javax.net.ssl.HttpsURLConnection;

import java.util.Scanner;
import java.util.Map;
import java.util.List;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Date;
import java.util.Base64;

import java.io.OutputStream;
import java.io.File;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Represents a student's authenticated connection to a particular
 * submission repository.
 * @author Thomas Shields
 * @version 2.0
 */
public class StudentSubmission {
    private static final String BASE = "https://github.gatech.edu/api/v3/";

    private String user;
    private String base64Auth;
    private String repo;
    private Map<String, String> headers;

    public StudentSubmission(String u, String b, String r) {
        this(u, b, r, null);
    }

    public StudentSubmission(String u, String b, String r, String c) {
        user = u;
        base64Auth = b;
        repo = r;
        headers = new HashMap<>();
        if (null != c)
            headers.put("X-GitHub-OTP", c);
    }

    public String getUser() {
        return user;
    }
    public String getRepo() {
        return repo;
    }
    private int request(String path, String type, String content)
        throws Exception {
        return doRequest(path, type, base64Auth, content, null, headers, null);
    }
    private int request(String path, String type, String content,
        StringBuilder out) throws Exception {
        return doRequest(path, type, base64Auth, content, out, headers, null);
    }

    private int request(String path, String type, String content,
        Map<String, List<String>> respHeadersOut) throws Exception {
        return doRequest(path, type, base64Auth, content, null,
            headers, respHeadersOut);
    }

    public static int doRequest(String path, String type, String auth,
        String content, StringBuilder sb, Map<String, String> headers,
        Map<String, List<String>> responseHeadersOut)
        throws IOException, MalformedURLException {

        HttpsURLConnection conn = (HttpsURLConnection) new URL(BASE + path)
            .openConnection();
        conn.setInstanceFollowRedirects(false);

        try {
            conn.setRequestMethod(type);
            conn.setRequestProperty("Authorization", "Basic " + auth);
            if (null != headers) {
                for (Entry<String, String> header : headers.entrySet()) {
                    conn.setRequestProperty(header.getKey(), header.getValue());
                }
            }

            if (!type.equals("GET")) {
                conn.setDoOutput(true);
                if (content.length() > 0) {
                    OutputStream os = conn.getOutputStream();
                    os.write(content.getBytes());
                    os.close();
                } else {
                    conn.setFixedLengthStreamingMode(0);
                }
            }
            if (null != sb) {
                sb.append(new Scanner(conn.getInputStream()).useDelimiter("\\A")
                    .next());
            }
            if (null != responseHeadersOut) {
                responseHeadersOut.putAll(conn.getHeaderFields());
            }

        } catch (IOException e) {
            e = e;
        }
        return conn.getResponseCode();
    }

    public static boolean testAuth(String base64)
        throws TwoFactorAuthException, MalformedURLException, IOException {
        Map<String, List<String>> headers = new HashMap<>();
        int code = doRequest("", "GET", base64, "", null, null, headers);
        if (null != headers.get("X-GitHub-OTP"))
            throw new TwoFactorAuthException();
        return code != 401;
    }

    public static boolean testTwoFactorAuth(String base64, String code)
        throws IOException, MalformedURLException {
        Map<String, String> auth = new HashMap<>();
        auth.put("X-GitHub-OTP", code);
        return doRequest("", "GET", base64, "", null, auth, null) != 401;
    }

    public void createRepo() throws Exception {
        if (request(String.format("repos/%s/%s", user, repo), "GET", "")
            == 404) {
            request("user/repos", "POST",
                String.format("{\"name\":\"%s\",\"private\": true}", repo));
        }
    }

    /**
     * Downloads the repo into the specified zip file
     * @param fileName the base name of the zip file to save into
     */
    public void download(String fileName)
        throws Exception {
        Map<String, List<String>> respHeaders = new HashMap<>();
        int code = request(String.format(
            "repos/%s/%s/zipball", user, repo), "GET", "", respHeaders);
        if (code == 404) throw new Exception("404 Not Found");
        try (
            BufferedInputStream bis = new BufferedInputStream(new URL(
                respHeaders.get("Location").get(0)).openStream());
            FileOutputStream fos = new FileOutputStream(
                new File(fileName + ".zip"));
        ) {
            final byte[] data = new byte[1024];
            int count;
            while ((count = bis.read(data, 0, 1024)) != -1) {
                fos.write(data, 0, count);
            }
        } catch (Exception e) {
            throw new Exception("Encountered error while downloading "
                + user + "'s submission", e);
        }
    }

    /**
     * Forks the repo using the StudentSubmissions' credentials
     * E.g, you can use someone else's auth for the submission
     * and then use it to fork
     */
    public int fork() throws Exception {
        return request(String.format(
            "repos/%s/%s/forks", user, repo), "POST", "");
    }

    public int fork(String org) throws Exception {
        StringBuilder sb = new StringBuilder();
        return request(String.format(
            "repos/%s/%s/forks?organization=%s", user, repo, org),
            "POST", "", sb);
    }

    public int delete() throws Exception {
        return request(String.format(
            "repos/%s/%s", user, repo), "DELETE", "");
    }

    public boolean removeCollab(String collab) throws Exception {
        return request(String.format(
            "repos/%s/%s/collaborators/%s", user, repo, collab), "DELETE", "")
            == 204;
    }

    /**
     * Adds someone (a TA, for example)
     * as a collaborator
     * @param collab the user name  to add
     * @return whether not the operation succeeded
     */
    public boolean addCollab(String collab) throws Exception {
        String path = String.format("repos/%s/%s/collaborators/%s", user, repo,
            collab);
        return request(path, "PUT", "") == 204;
    }


    /**
     * submits a file to the user's submission repo
     * @param f     the file to submit
     * @param message the commit message
     * @return if succesful
     */
    public boolean pushFile(File f, String message) throws Exception {
        StringBuilder sb = new StringBuilder();
        String sha = "";
        if (request(String.format("repos/%s/%s/contents/%s",
                user, repo, f.getName()), "GET", "", sb) != 404) {
            sha = sb.toString().split("sha\":\"")[1].split("\"")[0];
        }

        byte[] fileContents = new byte[(int) f.length()];
        new DataInputStream(new FileInputStream(f)).readFully(fileContents);

        message = new Date().toString() + " " + message;
        //TODO: json may not really be necessary? but add it? idk
        String json = String
            .format("{\"path\":\"%s\",\"message\":\"%s\",\"content\":\"%s\","
                + "\"sha\": \"%s\"}", f.getName(), message, Base64.getEncoder()
                    .encodeToString(fileContents), sha);
        int resp = request(String.format("repos/%s/%s/contents/%s",
            user, repo, f.getName()), "PUT", json);
        return resp == 200 || resp == 201;
    }
}
