var t = async_load[? "type"];

if (t == "cas_rewarded") {
    var st = async_load[? "state"]; // shown / closed / failed / complete
	show_debug_message([st])
    if (st == "complete") {
        show_debug_message("Complete Reward")
    }
    var err = async_load[? "error"];
}