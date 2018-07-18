package com.autonomy.find.api.datasource;

import java.util.ResourceBundle;

import org.apache.commons.dbcp.BasicDataSource;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptionAwareDataSource extends BasicDataSource {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(EncryptionAwareDataSource.class);

	@Override
	public synchronized void setPassword(String password) {

		String passkey = null;
		try {
			ResourceBundle rb = ResourceBundle.getBundle("passkey");
			passkey = rb.getString("passwordSalt");
		} catch (Exception e) {
			LOGGER.error("Error in retaining the passkey file", e.getMessage());
		}
		if (password.startsWith("ENC(")) {
			StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
			encryptor.setPassword(passkey);
			super.setPassword(encryptor.decrypt(password.substring(
					password.indexOf("(") + 1, password.indexOf(")"))));
		} else {
			super.setPassword(password);
		}
	}

}
