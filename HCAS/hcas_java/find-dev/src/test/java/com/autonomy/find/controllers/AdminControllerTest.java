package com.autonomy.find.controllers;

import java.util.Random;
import java.util.ResourceBundle;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.autonomy.find.services.WebAppUserService;
import com.autonomy.find.services.admin.AdminService;
import com.autonomy.find.util.LoginStatus;

@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("default")
@ContextConfiguration(locations = { "file:src/main/webapp/WEB-INF/applicationContext.xml","classpath:/test-applicationContext.xml" })
public class AdminControllerTest {
    
    
   @Autowired
   AdminService adminService;
   
   private ResourceBundle rb;

   private Random randomint = new Random();

   private int randomNumber = randomint.nextInt();

   @Autowired
   private WebAppUserService webappService;
   @Before
   public void setUp() {
       this.rb = ResourceBundle.getBundle("com.autonomy.find.test");
       webappService.registerUser(this.rb.getString("lock.username")
               + randomNumber, this.rb.getString("lock.password"));
   }
   @Test
   public void LockUnlockTest(){
       
       // Calling unlockUser api with username and unlock=false to lock the user
       adminService.unlockUser(this.rb.getString("lock.username")
               + randomNumber, false);
       // Asserting that UserDetail API returns locked=true with username
       Assert.assertTrue(adminService.getUser(this.rb.getString("lock.username")
               + randomNumber).getUserDetailsByUsername(this.rb.getString("lock.username")
               + randomNumber).getLocked());
       // Unlocking the user using adminService unlockUser api
       adminService.unlockUser(this.rb.getString("lock.username")
               + randomNumber, true);
       // Asserting that UserDetail API locked = false
       Assert.assertFalse(adminService.getUser(this.rb.getString("lock.username")
               + randomNumber).getUserDetailsByUsername(this.rb.getString("lock.username")
               + randomNumber).getLocked());
   }
   @After
   public void tearDown() {
        webappService.deleteUser(this.rb.getString("lock.username")+randomNumber);
   }


}
