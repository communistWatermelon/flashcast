// Set up context menus at install time.
chrome.runtime.onInstalled.addListener(function() {
  var context = "page";
  var title = "Send Page with FlashCast";
  var id = chrome.contextMenus.create({"title": title, "contexts":[context],
                                         "id": "context" + context});  
});

chrome.runtime.onInstalled.addListener(function() {
  var context = "link";
  var title = "Send Link with FlashCast";
  var id = chrome.contextMenus.create({"title": title, "contexts":[context],
                                         "id": "context" + context});  
});

chrome.browserAction.onClicked.addListener(function(tab) {
    chrome.tabs.create({ url: "http://localhost:8080" });
});

// add click event
chrome.contextMenus.onClicked.addListener(onClickHandler);

// The onClicked callback function.
function onClickHandler(info, tab) {
	chrome.extension.getBackgroundPage().console.log(info)
	chrome.extension.getBackgroundPage().console.log(tab)

	if (info.menuItemId === "contextpage") {
		var url = "localhost:8080/?q=" + encodeURIComponent(info.pageUrl);  
		window.open(url, '_blank');
	} else if (info.menuItemId === "contextlink") {
		var url = "localhost:8080/?q=" + encodeURIComponent(info.linkUrl);  
		window.open(url, '_blank');

	}
  // var sText = info.selectionText;
  // var url = "https://www.google.com/search?q=" + encodeURIComponent(sText);  
  // window.open(url, '_blank');
};