package org.linghu.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenBcrypt {
    public static void main(String[] args) {
        String pwd = args.length > 0 ? args[0] : "string";
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(pwd);
        System.out.println(hash);
    }
}
