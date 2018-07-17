/*
	jQuery document ready.
*/

$(document).ready(function()
{
	$('#startTimeInput').keyup(function()
	{
		$('#resultForStart').html(checkForStart($('#startTimeInput').val()));
	})
	
	$('#endTimeInput').keyup(function()
	{
		$('#resultForEnd').html(checkForEnd($('#endTimeInput').val()));
	})
	
	$("#startTimeInput").blur(function() {
		var startTimeValue = $('#startTimeInput').val();
		var endTimeValue = $("#endTimeInput").val();
		
		if(endTimeValue != "" && startTimeValue != "" && startTimeValue>=endTimeValue){
			if (startTimeValue==endTimeValue){
				$('#resultForEnd').removeClass();
				$('#resultForEnd').addClass('notValid');
				$('#resultForEnd').html('End Time can not be equal to Start time');
				$('#resultForStart').removeClass();
				$('#resultForStart').addClass('notValid');
				$('#resultForStart').html('Start Time can not be equal to End time');
			}
			else{
				$('#resultForEnd').removeClass();
				$('#resultForEnd').addClass('notValid');
				$('#resultForEnd').html('You entered a value which is less than start time');
				$('#resultForStart').removeClass();
				$('#resultForStart').addClass('notValid');
				$('#resultForStart').html('You entered a value which is greater than end time');
			}
		}
		
		if (!startTimeValue.match(/^(?:[0-9]*[0-9][0-9]):[0-5][0-9]:[0-5][0-9]$/)){
			$('#resultForStart').removeClass();
			$('#resultForStart').addClass('notValid');
			$('#resultForStart').html('Invalid');
		}
	});
	
	$("#endTimeInput").blur(function() {
		var endTimeValue = $('#endTimeInput').val();
		var startTimeValue = $('#startTimeInput').val();
		
		if(startTimeValue != "" && endTimeValue != "" && startTimeValue>=endTimeValue){
			if (startTimeValue==endTimeValue){
				$('#resultForEnd').removeClass();
				$('#resultForEnd').addClass('notValid');
				$('#resultForEnd').html('End Time can not be equal to Start time');
				$('#resultForStart').removeClass();
				$('#resultForStart').addClass('notValid');
				$('#resultForStart').html('Start Time can not be equal to End time');
			}
			else{
				$('#resultForEnd').removeClass();
				$('#resultForEnd').addClass('notValid');
				$('#resultForEnd').html('You entered a value which is less than start time');
				$('#resultForStart').removeClass();
				$('#resultForStart').addClass('notValid');
				$('#resultForStart').html('You entered a value which is greater than end time');
			}
		}
		
		if (!endTimeValue.match(/^(?:[0-9]*[0-9][0-9]):[0-5][0-9]:[0-5][0-9]$/)){
			$('#resultForEnd').removeClass();
			$('#resultForEnd').addClass('notValid');
			$('#resultForEnd').html('Invalid');
		}
	});
	
	function checkForStart(startTimeValue){
		if (startTimeValue.length == 0){
			$('#resultForStart').removeClass();
			$('#resultForStart').addClass('notValid');
			return 'In HH:MM:SS format';
		}
		
		if (startTimeValue.match(/^(?:[0-9]*[0-9][0-9]):[0-5][0-9]:[0-5][0-9]$/)){
			$('#resultForStart').removeClass();
			$('#resultForStart').addClass('valid');
			return 'Valid Format';
		}
	}
	
	function checkForEnd(endTimeValue){
		if (endTimeValue.length == 0){
			$('#resultForEnd').removeClass();
			$('#resultForEnd').addClass('notValid');
			return 'In HH:MM:SS format';
		}
		
		if (endTimeValue.match(/^(?:[0-9]*[0-9][0-9]):[0-5][0-9]:[0-5][0-9]$/)){
			$('#resultForEnd').removeClass();
			$('#resultForEnd').addClass('valid');
			return 'Valid Format';
		}
	}
	
	//When the user clicks on close button of trim modal
	$("#closeTrim").on("click", function () {
		$('#resultForStart').removeClass();
		$('#resultForEnd').removeClass();
		$('#resultForStart').html('');
		$('#resultForEnd').html('');
	})
	
	//Check to see if the window is top if not then display button
	$(window).scroll(function(){
		if ($(this).scrollTop() > 100) {
			$('.scrollToTop').fadeIn();
		} else {
			$('.scrollToTop').fadeOut();
		}
	});
	
	//Click event to scroll to top
	$('.scrollToTop').click(function(){
		$('html, body').animate({scrollTop : 0},800);
		return false;
	});
});
