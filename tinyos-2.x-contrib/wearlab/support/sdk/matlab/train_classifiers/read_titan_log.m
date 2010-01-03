%    This file is part of Titan.
%
%    Titan is free software: you can redistribute it and/or modify
%    it under the terms of the GNU General Public License as 
%    published by the Free Software Foundation, either version 3 of 
%    the License, or (at your option) any later version.
%
%    Titan is distributed in the hope that it will be useful,
%    but WITHOUT ANY WARRANTY; without even the implied warranty of
%    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
%    GNU General Public License for more details.
%
%    You should have received a copy of the GNU General Public License
%    along with Titan. If not, see <http://www.gnu.org/licenses/>.
%
%    Copyright 2009 Clemens Lombriser

%
% reads in a titan logfile and 
%
function data = read_titan_log(strfile, samplePeriod)

input = dlmread(strfile);

% first two columns are time, input port
time = input(:,1);
node = input(:,2);

% the rest is 16bit data - multiply second byte by 256 and add first
accdata = zeros(size(input,1),(size(input,2)-3)/2);
for i=3:2:size(input,2)-1
    accdata(:,(i-1)/2) = input(:,i+1)*256 + input(:,i);
end

% are packets merged on the nodes?
if size(accdata,2) > 3
    % restore sampling times
    mult = floor(size(accdata,2)/3);
    accdata2 = zeros( size(accdata,1)*mult, 3 );
    time2 = zeros( size(accdata,1)*mult, 1);
    node2 = -ones( size(accdata,1)*mult, 1);
    accdata2 = zeros(1,3);
    time2 = [0];
    node2 = [-1];
    
    % copy data
    for i=1:size(accdata,1)
        for j=1:mult
            accdata2((i-1)*mult+j,:) = accdata(i, [j j+mult j+2*mult]);
            time2((i-1)*mult+j) = time(i) + (j-1)*samplePeriod;
            node2((i-1)*mult+j) = node(i);
        end
        
    end
    data = [time2' node2' accdata2];
else
    data = [time node accdata];
end

