//#demo-page為預設頁面的ID,請自行更改
//圖片id請自行更改 預設為:"#demo-img"
//文字id請自行更改 預設為:"#demo-text"
//<a>id請自行更改 預設為:"#adswipe"
//arrimg_src裡面放置圖片檔名,要加多少隨意
//arrimg_name:圖片下面的標題
//arrlink給圖片or標題超連結

var flag = 0;//作為標記 
var arrimg_src = new Array("asus01.jpg","asus02.jpg","asus03.jpg");//圖片檔名,要加多少隨意
//陣列要用小括號,另一種陣列寫法:var arrimg_src = [a,b,c]
var arrimg_name = new Array("ZenFone 5z","ZenFone 2","ZenFone 3"); //圖片下面的標題
var arrlink = new Array("https://www.asus.com/tw/Phone/ZenFone-5Z-ZS620KL/",
						"https://www.asus.com/tw/Phone/ZenFone_2_ZE551ML/",
						"https://www.asus.com/tw/Phone/ZenFone-3-ZE552KL/");//給圖片or標題超連結


$( document ).on( "pagecreate", "#new_product", function() {                   
    $( document ).on( "swipeleft swiperight", "#new_product", function( e ) {
        // We check if there is no open panel on the page because otherwise
        // a swipe to close the left panel would also open the right panel (and v.v.).
        // We do this by checking the data that the framework stores on the page element (panel: open).
        
            if ( e.type === "swipeleft" ) {
                flag ++;
				if (flag>2) {flag = 0} //flag的數字對應圖片數量
				$("#pimg02").fadeOut(1000,changeImg);//將原本圖片淡出,再將下一張淡入 ; 先做fadeOut在執行changImg
				
            
			} else if ( e.type === "swiperight" ) {
                flag --;
				if (flag<0) {flag = 2}
				$("#pimg02").fadeOut(1000,changeImg);
				
            }
        
    });
});


function changeImg(){
	img_src = "image/"+arrimg_src[flag];//產生圖片的路徑
	img_name ="ASUS "+arrimg_name[flag];
	img_link = arrlink[flag];//產生連結
	$("#pimg02").attr("src",img_src).fadeIn(500);   
	$("#ptext02").text(img_name);       
	$("#adswipe").attr("href",img_link);
}

/*
範例:
		<div data-role="page" id="demo-page">
		
			<div data-role="header" data-position="fixed" data-theme="b">
				<h1>Home</h1>
			</div>
		<!-- header end -->
			
			<div role="main" class="ui-content">
				<img src="image/dog04.jpg"  alt="" id="demo-img">
				<p id="demo-">出口危險內容簡介參加之間晶片目的智能</p>
			</div>
		<!-- main end -->
		
			<div data-role="footer" data-position="fixed" data-theme="b">
				<h3>footer</h3>
			</div>
		<!-- footer end -->
		</div>
	
*/				