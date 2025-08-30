var t = async_load[? "type"];

if (t == "cas_interstitial") {
    var st = async_load[? "state"]; // shown / closed / failed
    var err = async_load[? "error"];
    show_debug_message([st])
}