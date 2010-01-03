
clear all;

sides = {'north', 'east', 'south', 'west'};
measurements = [15 30 60 100 130 160 200];

for side=1:length(sides)
    node1_rssi = [];
    node2_rssi = [];
    for i=1:length(measurements)

        nodes_rssi = parseRssiLog(sprintf('%s_%03icm.log',sides{side},measurements(i)));

        node1_rssi = [node1_rssi; nodes_rssi{1}'];
        node2_rssi = [node2_rssi; nodes_rssi{2}'];

    end
    
    node1_sides{side} = node1_rssi;
    node2_sides{side} = node2_rssi;

    % plot for every side the decreasing RSSI values
    %figure;
    %strLegend = {'0 dB', '-1 dB', '-3 dB', '-5 dB', '-7 dB', '-10 dB', '-15 dB', '-25 dB'};
    %subplot(2,1,1), plot(measurements,node1_rssi,'+-')
    %legend(strLegend)
    %subplot(2,1,2), plot(measurements,node2_rssi,'+-')
    %legend(strLegend)
    %title(sprintf('RSSI %s',sides{side}));
end

% a plot showing RSSI levels over distance

rssi_min=[];
rssi_max=[];
for i=1:length(sides)
    for j=1:length(measurements)
        rssi_min(i,j) = node1_sides{i}(j,1);
        rssi_max(i,j) = node1_sides{i}(j,end);
    end
end

rssi_min_mean = mean(rssi_min);
rssi_max_mean = mean(rssi_max);


%%
figure;
h = plot(measurements, rssi_max_mean', '-k', ...
     measurements, rssi_max', '+k', ...
     measurements, rssi_min_mean', '--k', ...
     measurements, rssi_min', 'xk' );
strLegend = {'0dB output mean','0dB output measured', '-25dB output mean','-25dB output measured'};
order = [1,2,6,7];
legend(h(order), strLegend);
YLabel('Received Signal Strength');
XLabel('Distance [cm]');
set(gca,'XLim',[0 210])



