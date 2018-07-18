package com.autonomy.find.controllers.admin;

import com.autonomy.find.config.FindConfig;
import com.autonomy.find.config.LoginSettings;
import com.autonomy.find.dto.admin.LogFile;
import com.autonomy.find.services.admin.AdminService;
import com.autonomy.find.util.SessionHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Controller
@RequestMapping("/")
public class AdminController {

    @Autowired
    private SessionHelper sessionHelper;

    @Autowired
    AdminService adminService;

    @Autowired
    private FindConfig config;


    @RequestMapping({"/admin/adminview.do"})
    public ModelAndView adminview_get(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final HttpSession session
    ) throws IOException {
        Map<String, Object> model = new HashMap<String, Object>();
        final LoginSettings settings = config.getLoginSettings();
        model.put("ssoOn", settings.ssoLoginAllowed());
        model.put("autonomyRepository", settings.isAutonomy());
        return new ModelAndView("admin/adminview", "model", model);
    }

    @RequestMapping(value={"/admin/userdetails.do"}, params={"username"})
    public ModelAndView userdetails_get(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final HttpSession session,
            final String username
    ) throws IOException {

        HashSet<String> userRoles = adminService.getUsersRoles(username);
        HashSet<String> allRoles = adminService.getAllRoles();


        Map<String, Object> model = new HashMap<String, Object>();
        model.put("roles", userRoles);
        model.put("username", username);
        model.put("allRoles", allRoles);
        model.put("locked", adminService.getUser(username).getUserDetailsByUsername(username).getLocked());
        
        final LoginSettings settings = config.getLoginSettings();
        model.put("autonomyRepository", settings.isAutonomy());

        return new ModelAndView("admin/userdetails","model", model);

    }

    @RequestMapping({"/admin/manageRoles.do"})
    public ModelAndView roles_get(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final HttpSession session
    ) throws IOException {

        return new ModelAndView();

    }

    @RequestMapping(value= {"/admin/privilegeDetails.do"}, params={"privilege"})
    public ModelAndView privilegeDetails_get(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final HttpSession session,
            final String privilege
    ) throws IOException {

        HashSet<String> rolePrivilegeBelongsTo = adminService.getRolePrivilegeBelongsTo(privilege);
        HashSet<String> allRoles = adminService.getAllRoles();

        ModelAndView modelAndView = new ModelAndView();

        String rolesString = null;

        if(rolePrivilegeBelongsTo != null) {
            rolesString = StringUtils.join(rolePrivilegeBelongsTo, ',');
        }

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("roles", rolePrivilegeBelongsTo);
        model.put("rolesString", rolesString);
        model.put("privilegeName", privilege);
        model.put("allRoles", allRoles);

        return new ModelAndView("admin/privilegeDetails","model", model);

    }

    @RequestMapping(value= {"/admin/roleDetails.do"}, params={"roleName"})
    public ModelAndView roleDetails_get(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final HttpSession session,
            final String roleName
    ) throws IOException {

//        HashSet<String> rolePrivilegeBelongsTo = adminService.getRolePrivilegeBelongsTo(privilege);
//        HashSet<String> allRoles = adminService.getAllRoles();
//
//        ModelAndView modelAndView = new ModelAndView();
//
//        String rolesString = null;
//
//        if(rolePrivilegeBelongsTo != null) {
//            rolesString = StringUtils.join(rolePrivilegeBelongsTo, ',');
//        }
//
//        Map<String, Object> model = new HashMap<String, Object>();
//        model.put("roles", rolePrivilegeBelongsTo);
//        model.put("rolesString", rolesString);
//        model.put("privilegeName", privilege);
//        model.put("allRoles", allRoles);

        Map<String, Object> model = new HashMap<String, Object>();
        model.put("allUsers", adminService.getAllUsers().getUsersUsernames());
        model.put("allPrivileges", adminService.getAllPrivileges());
        model.put("users", adminService.getUsersWithRole(roleName));
        model.put("privileges", adminService.getPrivilegesForRole(roleName));
        model.put("roleName", roleName);
        return new ModelAndView("admin/roleDetails","model", model);

    }


    @RequestMapping({"/admin/logView.do"})
    public ModelAndView logView(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final HttpSession session
    ) throws IOException {

        return new ModelAndView();

    }

    @RequestMapping({"/admin/getAuditLogs.do"})
    public @ResponseBody
    List<LogFile> getAuditLogs(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final HttpSession session
    ) throws IOException {

        final RegexFileFilter regexFileFilter = new RegexFileFilter(config.getAuditFilePattern());

        final FileSystemResource logFileDir = new FileSystemResource(config.getAuditLogDir());
        final File logDir = logFileDir.getFile();
        final Collection<File> auditFiles = FileUtils.listFiles(logDir, regexFileFilter, TrueFileFilter.INSTANCE );

        final List<LogFile> logFiles = new ArrayList<LogFile>();

        for(final File auditFile : auditFiles) {
            if (auditFile.isFile()) {
                final LogFile logFile = new LogFile(auditFile.getName(), auditFile.lastModified(), FileUtils.byteCountToDisplaySize(auditFile.length()));
                final String relPath = auditFile.getCanonicalPath().replace(logDir.getCanonicalPath(), "").replaceAll("\\\\", "/").replaceFirst("^/", "");
                logFile.setFileUrl("viewAuditLog.do?logfile=" + URLEncoder.encode(relPath, "UTF-8"));

                logFiles.add(logFile);
            }
        }

        return logFiles;

    }

    @RequestMapping({"/admin/viewAuditLog.do"})
    public void getAuditLog(
            @RequestParam(value="logfile") final String logfile,
            final HttpServletResponse response,
            final HttpServletRequest request ) throws IOException{

        String logDirVal = config.getAuditLogDir();
        if (logDirVal != null && !(logDirVal.endsWith("\\") || logDirVal.endsWith("/"))) {
            logDirVal += "/";
        }

        final FileSystemResource logFileDir = new FileSystemResource(logDirVal);
        final Resource logFile = logFileDir.createRelative(logfile);

        response.setContentType(MediaType.TEXT_PLAIN_VALUE);

        final InputStream inStream;
        final OutputStream outStream;

        final String acceptedEncoding = request.getHeader("Accept-Encoding");
        if (acceptedEncoding != null && acceptedEncoding.indexOf("gzip") != -1) {
            response.setHeader("Content-Encoding", "gzip");
            inStream = logFile.getInputStream();
            outStream = logfile.endsWith(".gz") ? response.getOutputStream() :  new GZIPOutputStream(response.getOutputStream());

        } else {
            inStream = logfile.endsWith(".gz") ? new GZIPInputStream(logFile.getInputStream()) : logFile.getInputStream();
            outStream = response.getOutputStream();
        }


        IOUtils.copyLarge(inStream, outStream);
        outStream.flush();
        IOUtils.closeQuietly(inStream);
        IOUtils.closeQuietly(outStream);
    }


	
}
