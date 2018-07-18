package com.autonomy.find.api.controllers;

import com.autonomy.find.api.response.ResponseWithResult;
import com.autonomy.find.api.response.ResponseWithSuccessError;
import com.autonomy.find.services.AppService;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.Map;

@Controller
@RequestMapping("/api/app")
public class AppController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppController.class);

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String VERSION_DATA_NOT_FOUND = "Could not retrieve version information.";
    private static final String INCORRECT_FORMAT = "Version information could not be parsed due to incorrect formatting.";


    @Autowired
    private AppService appService;

    @RequestMapping("getVersionInfo.json")
    public
    @ResponseBody
    @SuppressWarnings("unchecked")
    ResponseWithSuccessError getVersionInfo() {
        String error = null;
        try {
            return new ResponseWithResult<Map<String, Object>>(
                    (Map<String, Object>) mapper.readValue(
                            appService.loadAppVersionData(), new TypeReference<Map<String, Object>>() {}));
        } catch (final IOException e) {
            LOGGER.error("Error during getVersionInfo.json", e);
            error = VERSION_DATA_NOT_FOUND;
        } catch (final Exception e) {
            error = INCORRECT_FORMAT;
        }

        return new ResponseWithSuccessError(false, error);
    }
}
