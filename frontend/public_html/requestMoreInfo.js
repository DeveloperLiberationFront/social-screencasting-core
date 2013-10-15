$(document).ready(function()
{
	$(".clickMe").on('click',function(){
		$(".moreInfo").removeClass("hidden");
		
	});
	
	$(".clickMe").on('mouseenter',function(){
		$(this).animate("hoverOver")
	});
	$(".clickMe").on('mouseleave',function(){
		$(this).removeClass("hoverOver")
	});

});