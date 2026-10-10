import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class HashGen {
  public static void main(String[] a) {
    BCryptPasswordEncoder e = new BCryptPasswordEncoder();
    for (String raw : new String[]{"Admin@123", "User@123"}) {
      String h = e.encode(raw);
      System.out.println(raw + " => " + h + "  verify=" + e.matches(raw, h));
    }
  }
}
