if (async_load[? "type"] == "cas_banner") 
{
	CAS_BannerShow();
	
    var st    = async_load[? "state"]; // loaded / failed / clicked / impression / shown / hidden / destroyed
    var where = async_load[? "place"]; // top/bottom/big
    var err   = async_load[? "error"];
	show_message_async([st]);
}