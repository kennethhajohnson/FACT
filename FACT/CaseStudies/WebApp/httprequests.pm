dtmc


const double y1 = 0.40530;
const double y2 = 0.59460;

const double x1 = 0.57900;
const double x2 = 0.00050;

const double w1 = 0.99980;

const double z1 = 0.25065;
const double z2 = 0.00125;

const double k1 = 0.99960;


module M1

q : [0..9] init 0;

[] q=0 -> y1:(q'=1) + y2:(q'=3) + (1-y1-y2):(q'=7);
[] q=1 -> 0.2:(q'=1) + 0.55:(q'=2) + 0.25:(q'=8);
[] q=2 -> 0.7:(q'=5) + 0.3:(q'=8);
[] q=3 -> x1:(q'=8) + x2:(q'=9) + (1-x1-x2):(q'=4);
[] q=4 -> w1:(q'=8) + (1-w1):(q'=9);
[] q=5 -> z1:(q'=6) + z2:(q'=9) + (1-z1-z2):(q'=8);
[] q=6 -> k1:(q'=8) + (1-k1):(q'=9);
[] q=7 -> 1:(q'=7);
[] q=8 -> 1:(q'=8);
[] q=9 -> 1:(q'=9);

endmodule


rewards "cost"
	q=1 : 1;
        q=2 : 2;
        q=3 : 1;
        q=4 : 1;
        q=5 : 1;
        q=6 : 4;
endrewards

rewards "time"
	q=4 : 4;
        q=6 : 7;
endrewards
