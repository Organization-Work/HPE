package com.autonomy.find.controllers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;
import java.util.ResourceBundle;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.autonomy.aci.client.annotations.IdolAnnotationsProcessorFactory;
import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.find.processors.GetConfigProcessor;
import com.autonomy.find.services.WebAppUserService;
import com.autonomy.find.util.LoginResult;
import com.autonomy.find.util.LoginStatus;

@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("default")
@ContextConfiguration(locations = { "file:src/main/webapp/WEB-INF/applicationContext.xml","classpath:/test-applicationContext.xml" })
public class FindControllerTest {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(FindControllerTest.class);

	@Autowired
	@Qualifier("autonomyConfig")
	private AciService communityService;

	@Autowired
	private IdolAnnotationsProcessorFactory processorFactory;

	private Random randomint = new Random();

	private int randomNumber = randomint.nextInt();

	@Autowired
	private WebAppUserService webappService;

	private ResourceBundle rb;

	@Before
	public void setUp() {
		this.rb = ResourceBundle.getBundle("com.autonomy.find.test");
		webappService.registerUser(this.rb.getString("lock.username")
				+ randomNumber, this.rb.getString("lock.password"));
	}

	@Test
	public void lockUserTest() {

		Properties prop = new Properties();
		//InputStream input = null;
		try {
		    AciParameters aciParams = new AciParameters("GetConfig");
		    String responseData =communityService.executeAction(aciParams,new GetConfigProcessor());
		    
			// Read the response from getconfigprocessor
			InputStream input = new ByteArrayInputStream(responseData.getBytes());
			// Load it into properties
			prop.load(input);
			   // Access the configured propertiess 
			int loginAttempts = prop.getProperty("LoginMaxAttempts") != null ? Integer
					.parseInt(prop.getProperty("LoginMaxAttempts")) : 0;
			int loginExpiry = prop.getProperty("LoginExpiryTime") != null ? Integer
					.parseInt(prop.getProperty("LoginExpiryTime")) : 1;
			// Make sure login expiry and login attempts are set
			Assert.assertNotNull(loginExpiry);
			Assert.assertNotNull(loginAttempts);
			for (int i = 0; i < loginAttempts; i++) {
				webappService.userLogin(this.rb.getString("lock.username")
						+ randomNumber, this.rb.getString("lock.password")
						+ "1");
			}
			if (loginAttempts != 0) {
				// Assert we get an error for locked user with more than 3
				// failed attempts of wrong password
				
				LoginResult r = webappService
				.userLogin(this.rb.getString("lock.username")
						+ randomNumber,
						this.rb.getString("lock.password"));
				
				Assert.assertTrue(LoginStatus.ERROR == r.getLoginStatus());
				Assert.assertTrue(r.getMessage() != null && r.getMessage().contains("locked"));
			}

		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}

	}

	@After
	public void tearDown() {
		 webappService.deleteUser(this.rb.getString("lock.username")+randomNumber);
	}

}
