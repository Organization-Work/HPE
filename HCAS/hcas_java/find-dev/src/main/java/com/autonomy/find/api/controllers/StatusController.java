package com.autonomy.find.api.controllers;

import com.autonomy.find.api.response.ResponseStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/api/status")
public class StatusController {

	@RequestMapping("getServerStatus.json")
	public @ResponseBody
    ResponseStatus serverStatus() {
		return new ResponseStatus(true);
	}
    
}
