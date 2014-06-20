var userName, userEmail, userToken, authString;

$(document).ready(function(){
    userName = $("body").data("name");
    userEmail = $("body").data("email");
    userToken = $("body").data("token");
    authString = "?name=" + escape(userName) + "&email=" + escape(userEmail) + "token=" + escape(userToken);
  getRequests();
});

function getRequests() {
  var url = "http://screencaster-hub.appspot.com/api/" + escape(userEmail) + authString;
  $.ajax({
    type: "GET",
    url: url,
    data: {},
    success: showRequests
  })
}

function showRequests(requestsData) {
  console.log(requestsData)
}