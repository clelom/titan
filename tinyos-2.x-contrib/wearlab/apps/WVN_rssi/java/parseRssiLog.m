%
% Reads in a WVN_Rssi - measurement log and determines average received 
% power per node and signal strength
%
function [node_rssi] = parseRssiLog(strfile)

%clear all;
%A = dlmread('testlog.log',';',1,0);

A = dlmread(strfile',';',1,0);

% format of A: Source;Destination;Power;RSSI;Counter

%%

% seperate node readings
node1 = A(find(A(:,1)==1),:);
node2 = A(find(A(:,1)==2),: );

assert( size(node1,1) + size(node2,1) == size(A,1) );


% separate power measurements
powers = [31 27 23 19 15 11 7 3];
for i=1:length(powers)
    node1_pow{i} = node1( find(node1(:,3)==powers(i)),:);
    node2_pow{i} = node2( find(node2(:,3)==powers(i)),:);
    
    if (size(node1_pow{i},1) ~= 0)
        node1_rssiavg(i) = mean(node1_pow{i}(:,4));
    else
        node1_rssiavg(i) = NaN;
    end
    if (size(node2_pow{i},1) ~= 0)
        node2_rssiavg(i) = mean(node2_pow{i}(:,4));
    else
        node2_rssiavg(i) = NaN;
    end
    
end

node_rssi{1} = node1_rssiavg';
node_rssi{2} = node2_rssiavg';



