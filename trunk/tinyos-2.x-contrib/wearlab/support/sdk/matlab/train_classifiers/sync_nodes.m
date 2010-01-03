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

function logs = sync_nodes(in,samplePeriod)

nodes = max(in(:,2));
logindex=0;
for i=0:nodes
    nodein = in(in(:,2)==i,:); % extract data of the node
    
    % check whether the node exists at all
    if size(nodein,1) == 0
        continue;
    else
        logindex = logindex+1;
    end

    % store data of interest
    logs{logindex}.time  = nodein(:,1);
    logs{logindex}.data  = nodein(:,3:5);
    
    % find all missing packets and replace them by the last entry
    lost = find((logs{logindex}.time(2:end) - logs{logindex}.time(1:end-1)) > 2*samplePeriod);
    fprintf(1,'Node %i: %i lost items in %i received \n', i, length(lost), length(logs{logindex}.time));
    while length(lost) > 0
        
        % create enries (create missing times, insert data)
        newtime = [logs{logindex}.time(lost(1)):samplePeriod:logs{logindex}.time(lost(1)+1)]';
        newdata = [];
        for j=1:length(newtime)
            newdata = [newdata;logs{logindex}.data(lost(1),:)];
        end
        
        % insert data
        logs{logindex}.time = [logs{logindex}.time(1:lost(1));newtime;logs{logindex}.time(lost(1)+1:end)];
        logs{logindex}.data = [logs{logindex}.data(1:lost(1),:);newdata;logs{logindex}.data(lost(1)+1:end,:)];
        
        lost = find((logs{logindex}.time(2:end) - logs{logindex}.time(1:end-1)) > 2*samplePeriod);
    end
    
end

%%
% resample nodes

% create virtual time axis
for logindex=1:length(logs)
    maxtime(logindex) = logs{logindex}.time(end);
end
time = 0:samplePeriod:max(maxtime);

for i=1:length(logs)
    
    data = zeros(length(time),3);
    
    for j=1:length(time)
        
        lower = max(find( logs{i}.time <= time(j) ));
        upper = min(find( logs{i}.time >  time(j) ));
        
        if (length(lower) == 0) 
            data(j,:) = logs{i}.data(1); % copy first data item if no earlier data is available
            continue;
        end
        if (length(upper) == 0)
            data(j,:) = logs{i}.data(end); % copy last data item if no later data is available
            continue;
        end
        
        % compute ratio of virtual time of difference between lower and
        % upper time
        lowerdist = time(j) - logs{i}.time(lower);
        upperdist = logs{i}.time(upper) - time(j);
        lowerdist = lowerdist/(lowerdist+upperdist);
        
        % data is added to lower value according to the ratio
        diff = logs{i}.data(upper,:) - logs{i}.data(lower,:);
        data(j,:) = logs{i}.data(lower,:) + round((diff)*lowerdist);
        
    end
    logs{i}.data = data;
    logs{i}.time = time;
    
end


