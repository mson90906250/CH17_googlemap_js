var opendata = [];//用於接收opendata的資料

		$(function(){
		//ajax接收opendata
			$.ajax({
				type:"GET",
				url:"http://192.168.60.101/website/get_opendata_parkinglot.php",
				dataType:"json",
				success:getData,
				error:function(){
					alert("無資料接收");
				}
			});
		});//end fun

		//算出距離的公式
		function getDistance(Lat1, Long1, Lat2, Long2){
			ConvertDegreeToRadians=function(degrees){
				return (Math.PI/180)*degrees;
			}
			var Lat1r = ConvertDegreeToRadians(Lat1);
			var Lat2r = ConvertDegreeToRadians(Lat2);
			var Long1r = ConvertDegreeToRadians(Long1);
			var Long2r = ConvertDegreeToRadians(Long2);

			var R = 6371; // 地球半徑(km)
			var d = Math.acos(Math.sin(Lat1r) * Math.sin(Lat2r) +
			 Math.cos(Lat1r) * Math.cos(Lat2r) * Math.cos(Long2r-Long1r)) * R;
			return d; // 兩點的距離 (KM)
		}

		//將接收到的資料傳給變數接收
		function getData(data){
			//做之前需先檢查資料的結構,有些是很像json結構,但卻在層多了個大括號
			opendata = data.parkingLots;

			createMap();
			
		}

		function createMap(){
			var infowindow = new google.maps.InfoWindow();
			//設定地圖的中心點
			var map_div = document.getElementById("map_div");
			//取得經緯度
			var lat = 24.170566;
			var lng = 120.609932;

			//方法會將坐標轉成複雜的數字
			var latlng = new google.maps.LatLng(lat,lng);


			var gmap = new google.maps.Map(map_div,{//選擇要呈現地圖的框架
				zoom:8,//放大的倍率(8倍大約可以將整個台灣顯示出來)
				center:latlng,//設置地圖的中心點
				mapTypeId:google.maps.MapTypeId.ROADMAP//設置地圖種類
			});


			//設置多個地標
			var marker = [];
	
			for(var i = 0; i<opendata.length; i++){


			latlng = new google.maps.LatLng(opendata[i].wgsY,opendata[i].wgsX);

			//建立標記記錄指定的地點
				marker[i] = new google.maps.Marker({
				position:latlng,
				icon:"image/icon/flag.png",
				map:gmap,
				title: opendata[i].parkName,
			});

				//給與標記一個訊息視窗
				google.maps.event.addListener(marker[i],"click",function(event){
					
					//取得在地圖上觸發點擊的坐標
					var lat = event.latLng.lat();//取維度
					var lng = event.latLng.lng();//取經度

					//算出與各地標距離,並從中選取最短且符合設定的距離
					for(var j=0; j<opendata.length; j++){

						
						//取得距離
						var dist = getDistance(lat,lng,opendata[j].wgsY,opendata[j].wgsX);
						
						//若距離符合設定則顯示訊息視窗
						if (dist<0.05) {
							infowindow.setContent("<div><p>Name= "+opendata[j].parkName+"</p><p>剩餘車位= "+opendata[j].surplusSpace+"</p></div>");
							infowindow.open(gmap,marker[j]);//設定訊息視窗出現的位置
						}//if					
					}//for
					
				});

			}//for
			
		}