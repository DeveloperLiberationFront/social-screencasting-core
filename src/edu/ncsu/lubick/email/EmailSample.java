package edu.ncsu.lubick.email;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Scanner;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import edu.ncsu.lubick.localHub.forTesting.TestingUtils;


public class EmailSample {
	private static final String EMAIL_PATH = "/etc/email.ini";
	private static final Logger logger = Logger.getLogger(EmailSample.class);
	
	private String username;
	private String password;
	private boolean emailInitialized = false;
	private Session session;

	
	public EmailSample()
	{
		try(Scanner scanner = new Scanner(EmailSample.class.getResourceAsStream(EMAIL_PATH),
				StandardCharsets.UTF_8.name())) {
			username = scanner.nextLine();
			password = scanner.nextLine();
			emailInitialized = true;
		} catch (Exception e) {
			logger.fatal("Could not set up email");
			username = "notInitialized";
			password = "notInitialized";
		}
		
		logger.debug(username);
		logger.debug(password);
		setUpSession();
		
	}
	

	private void setUpSession()
	{
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");

		this.session = Session.getInstance(props, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		});
	}


	private void sendSampleEmail()
	{
		if (!emailInitialized) {
			logger.error("Could not send email because auth wasn't initialized");
			return;
		}
		try {

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(username));
			message.setRecipients(Message.RecipientType.TO,	InternetAddress.parse("kjlubick@ncsu.edu"));
			message.setSubject("Java Test email 2");
			message.setText("Dear Kevin,\n\n Please check if you got this!");

			Transport.send(message);
		} catch (MessagingException e) {
			logger.error("Could not send email", e);
		}
	}


	public static void main(String[] args) {
		TestingUtils.makeSureLoggingIsSetUp();
		EmailSample emailSample = new EmailSample();
		emailSample.sendSampleEmail();
		
		logger.info("Done");
	
	}

}