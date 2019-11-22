package org.voyanttools.trombone.mail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Mailer {

	public static void sendMail(String to, String subject, String body) throws MessagingException {
		if (Mailer.isEmailAddressValid(to) == false) {
			throw new MessagingException("Invalid email address for recipient.");
		}
		
		InputStream mailerPropsStream = Mailer.class.getResourceAsStream("/org/voyanttools/trombone/mail/mailer.properties");
		Properties secretProps = new Properties();
		try {
			secretProps.load(mailerPropsStream);
		} catch (IOException e) {
			throw new MessagingException("Error", e);
		}

		final String username = secretProps.getProperty("username");
		final String password = secretProps.getProperty("password");
		final String from = secretProps.getProperty("from");

		
		Properties mailProps = new Properties();
		mailProps.put("mail.smtp.auth", "true");
		mailProps.put("mail.smtp.starttls.enable", "true");
		mailProps.put("mail.smtp.host", "smtp.gmail.com");
		mailProps.put("mail.smtp.port", "587");

		Session session = Session.getInstance(mailProps, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});

		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(from));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
		message.setSubject(subject);
		message.setText(body);
		message.setHeader("Content-Type", "text/html; charset=UTF-8");
		message.setSentDate(new Date());
		
		Transport.send(message);
		 
	}

	public static boolean isEmailAddressValid(String email) {
		boolean result = true;
		try {
			InternetAddress emailAddr = new InternetAddress(email);
			emailAddr.validate();
		} catch (AddressException ex) {
			result = false;
		}
		return result;
	}

}
