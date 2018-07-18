$(document).ready(function () {
 


  


			var $myfloat = "left" 

			if ($('html.right-layout').length > 0) { 
			var $myfloat = "right" 
			}


			//get the contents of the second header line and assign to variable//
			var $div = $('<div>');
			$div.load('Content/Resources/MasterPages/_HP_HTML5_bannerTitle.htm #header-title2', function () {

			//remove all spaces and returns from the result//
			test = $(this).text()
			test = test.replace(/\s+/g, '');
			test = test.replace(/[\n\r]/g, '');

			//set the base format for the header text//
			$("#header-content").css({
			"visibility": "visible",
			"float": "left",
			"left": "20px",
			"padding-left": "1em",
			"font-family": "'HP Simplified', HPSimplifiedLocal, Arial, Tahoma, 'Lucida Sans Unicode', sans-serif;",
			//"font-weight": "bold",//
			"float" : $myfloat
			 
			});

			if (test != "") { //if the second header variable is null, set formats for a one-line banner//
			// add styles for header text //
			$("#header-content").css({
			"margin-top": "0.8em",
			"font-size": "1.2em"
			});

			} else { //if the second header variable is not null, set formats for a two-line banner//
			// add styles for header text //
			$("#header-content").css({
			"margin-top": "0.5em",
			"font-size": "2em"
			});
			}
			});


			// add divs for header text and for logo //
			$("#header>a").after('<div id="header-content"></div>');
			$("#header>a").after('<div id="header-logo"></div>');

			// load the contents of the #header-content from Content/Resources/MasterPages/_HP_HTML5_bannerTitle.htm file into #header-content //
			$("#header-content").load("Content/Resources/MasterPages/_HP_HTML5_bannerTitle.htm #header-content  > *");

			//write the link to the standard logo file//
			$("#header-logo").html("<img src='Content/Resources/MasterPages/mf_logomark_blue_small.png' />"); //write the call to the bannar graphic and format the div.


			// add styles to position the logo //
			$("#header-logo").css({
			"visibility": "visible",
			"float" : $myfloat,
			"left": "20px",
			"padding-left": "1em",
			"margin-top": "1em"
			});


			

			$("span.tabs-nav-text").css({"cursor": "default"}); //QCCR1K4337
			$("#contentBodyInner").css({
			"border-left-color":"white",
			"border-right-color":"white",
			"margin-top":"0.5em",
			  }); 

 			$("#contentBody").css({"margin-left": "1.5em"}); //QCCR1K4337


			$("div.search-filter-content").css({"border":"1px solid #E5E8E8"});   /*QCCR1K4340*/

			//Get the name of the helpset's default file name
			var url = window.location.pathname;
			var defaultFile = url.substring(url.lastIndexOf('/')+1);

			if (defaultFile  != "Default.htm") {//If the default filename is not Default.htm, reset the link using the correct default file name
				var link = document.getElementById("searchLink");
				var path= "./" + defaultFile + "#Resources/MasterPages/_HP_aboutSearch.htm";
				link.setAttribute("href", path);
}

});



		