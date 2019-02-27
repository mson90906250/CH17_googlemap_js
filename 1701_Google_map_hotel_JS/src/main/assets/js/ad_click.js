
$(function(){//相當於java的main()
	$("#showprev").bind("click",showprev);
	//與id為#showprev的物件做連結,bind等同於listener,裡頭有兩個參數:第一個是要監聽何者事件,第二個為當事件觸發時要執行的方法
	$("#shownext").bind("click",shownext);		
});

var flag = 0;//作為標記 
var arrimg_src = new Array("asus01.jpg","asus02.jpg","asus03.jpg");
//陣列要用小括號,另一種陣列寫法:var arrimg_src = [a,b,c]
var arrimg_name = new Array("ZenFone 5z","ZenFone 2","ZenFone 3");
var arrlink = new Array("https://www.asus.com/tw/Phone/ZenFone-5Z-ZS620KL/",
						"https://www.asus.com/tw/Phone/ZenFone_2_ZE551ML/",
						"https://www.asus.com/tw/Phone/ZenFone-3-ZE552KL/");//給圖片or文字超連結
var img_src,img_name;	
	
	function showprev(){
	flag --;
	if (flag<0) {flag = 2}
	img_src = "image/"+arrimg_src[flag];
	img_name ="ASUS "+arrimg_name[flag];
	img_link = arrlink[flag];
	
	$("#pimg02").attr("src",img_src);
	//attr可用來修改element裡的attribute的值
	//一此題為例,attr修改了img中的src
	$("#ptext02").text(img_name);
	$("#adclick").attr("href",img_link);
}	

function shownext(){
	flag ++;
	if (flag>2) {flag = 0}
	img_src = "image/"+arrimg_src[flag];
	img_name ="ASUS "+arrimg_name[flag];
	img_link = arrlink[flag];
	
	$("#pimg02").attr("src",img_src);
	$("#ptext02").text(img_name);
	$("#adclick").attr("href",img_link);

}
//setInterval(showprev,2000);	
/*setInterval(
	function(){showprev();},5000);*/
