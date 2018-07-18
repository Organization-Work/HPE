package com.autonomy.find.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.autonomy.find.api.datasource.EncryptionAwareDataSource;

@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("default")
@ContextConfiguration(locations = { "file:src/main/webapp/WEB-INF/applicationContext.xml" })
public class databaseEncryptionTest {
	
	private EncryptionAwareDataSource ds;
	@Before
	public void setup(){
		
		
	}
	
	@Test
	public void encryptionTest(){
		ResourceBundle rs = ResourceBundle.getBundle("hca");
		// Get the db password string from class path
		String dbPassword = rs.getString("find-db.password");
	// Check if dbPassword starts with ENC
		if(dbPassword.startsWith("ENC")){
			
			ds = new EncryptionAwareDataSource();
			try {
				Connection con =ds.getConnection();
				Statement stat = con.createStatement();
				Assert.assertTrue(stat.execute("select * from find.user_role"));
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				Assert.fail(e.getMessage());
			}
		}
		
	}
	@After
	public void tearDown(){
		
	}
}
