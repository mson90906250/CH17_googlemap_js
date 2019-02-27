

//最好搭配for_ad.css 並為pimg01及ptext01做框架<div>分別為class="pimg";class="ptext"


/*
範例:
	<div class="pimg">
		<img src="image/asus01.jpg" alt="" id="pimg01">
	</div>
	<div class="ptext">
		<a href="https://openhome.cc/Gossip/Java/index.html" id="a01">
			<p id="ptext01">ASUS ZenFone5z</p></a>
 	</div>

*/


var flag = 0;//作為標記,在圖片及文字上做標記 


var arrimg_src = new Array("asus01.jpg","asus02.jpg","asus03.jpg");//放置圖片的檔名or路徑,請自行更改
var arrimg_name = new Array("ZenFone 5z","ZenFone 2","ZenFone 3");//每個圖片的名稱
var arrlink = new Array("https://www.asus.com/tw/Phone/ZenFone-5Z-ZS620KL/",
						"https://www.asus.com/tw/Phone/ZenFone_2_ZE551ML/",
						"https://www.asus.com/tw/Phone/ZenFone-3-ZE552KL/");//給圖片or文字超連結

var img_src,img_name,img_link;	
	
function show_ad(){
	flag --;
	if (flag<0) {flag = 2}//數字依陣列的長度而定
	img_src = "image/"+arrimg_src[flag];//做出完整的相對路徑,記得改成自己的
	img_name ="ASUS "+arrimg_name[flag];//做出完整的名稱,記得改成自己的
	img_link = arrlink[flag];
	
	$("#pimg01").attr("src",img_src);//給<img>設定id="pimg01"
	$("#ptext01").text(img_name);//給<p>id="ptext01"
	$("#a01").attr("href",img_link);//給<a>id="a01"	
}	

//setInterval(show_ad,2000); //看情況再用,最好打在or複製到主程式裡	

/*setInterval(
	function(){show_ad();},5000);*/

//attr可用來修改element裡的attribute的值,以此題為例,attr修改了img中的src:將原本的src改成img_src 

//陣列要用小括號,另一種陣列寫法:var arrimg_src = [a,b,c]


