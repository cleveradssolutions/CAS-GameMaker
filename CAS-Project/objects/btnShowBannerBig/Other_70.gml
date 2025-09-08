if (async_load[? "type"] == "cas_banner") 
{
    var st    = async_load[? "state"]; // loaded / failed / clicked / impression / shown / hidden / destroyed
    var where = async_load[? "place"]; // top/bottom/big
    var err   = async_load[? "error"];
}