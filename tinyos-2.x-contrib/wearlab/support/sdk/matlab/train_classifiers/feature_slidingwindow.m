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
%    Copyright 2009 Clemens Lombriser and Andreas Bulling

%FUNCTION returns a featurevector, This vector is calculated by sliding a window over data and calculating feature on every window
function [feature label]=feature_slidingwindow(data,featuretype,WINDOWSIZE,datalabels)

% some input checks
if size(data,1) ~= size(datalabels,1)
    error 'data and labels must have same number of samples';
end

if size(datalabels,2) ~= 1
    error 'this code only works with 1 label';
end

[datalength numvectors]= size(data);
feature=[];

% check on feature type
if strcmp(featuretype,'corr') || strcmp(featuretype,'coh')
    feature = zeros( datalength, 1 );
else
    feature = zeros( datalength, numvectors );
end

var_data = var(data); % needed on some features

label = zeros(datalength, size(datalabels,2));

% segment and compute label and features
for i=WINDOWSIZE:datalength
    
    start_idx=i-WINDOWSIZE+1;
    
    window_indices=start_idx:i;
    
    dataWnd = data(window_indices,:);
    labelWnd = datalabels(window_indices,:);

    % get the label - the one most often seen in the window
    if max(labelWnd) < 1
        label(i) = 0;
    else
        n=hist(labelWnd,max(labelWnd));
        n=find(n==max(n));
        label(i) = n(1); % if multiple are found, pick the first one
    end
   
    % now compute features
    switch featuretype
    case 'mean'
        feature(start_idx,:)= mean( dataWnd );
    case 'median'
        feature(start_idx,:)= median( dataWnd );
    case 'medianfilter'
    	feature(window_indices,: ) = dataWnd - median( dataWnd ); 
    case 'abs'
        feature(window_indices,:)= abs( dataWnd );
    case 'var'
        feature(start_idx,:)= var( dataWnd );
    case 'max'
        feature(start_idx,:)= max( dataWnd );
    case 'min'
        feature(start_idx,:)= min( dataWnd );
    case 'pkcnt'
        feature(start_idx,:)= sum( (dataWnd > 4) ); % The get_peaks script initialises everything to 4 ..
	case 'begenddiff'
		feature(start_idx,:)=dataWnd(end,:)-dataWnd(1,:);
    case 'zeroxing' % zero crossings with margin around mean
        winmean = mean(dataWnd);
        margin = 0.20*var_data;
        
        zx = zeros(size(winmean)); % count crossings
        currow = dataWnd(1,:)-winmean; % get start row
        lastval = currow; % remember last values
        for j=2:size(dataWnd,1) % step through samples
            currow = dataWnd(j,:)-winmean; % subtract mean
            
            % check on each value if it changed over the threshold
            for k=1:size(currow,2)
                if currow(k) > margin & lastval(k) < margin & abs(lastval(k)) >= margin % going up
                    lastval(k) = currow(k);
                    zx(k) = zx(k) + 1;
                elseif currow(k) < margin & lastval(k) > margin & abs(lastval(k)) >= margin % going down
                    lastval(k) = currow(k);
                    zx(k) = zx(k) + 1;
                elseif abs(lastval(k)) < margin % had not yet a value big enough to decide side
                    lastval(k) = currow(k);
                end
            end
        end
        feature(start_idx,:)= zx; % store result

    % Features with special configurations
    case 'corr'
        if size(dataWnd,2)~=2
            error 'feature "corr" only implemented for 2 column data';
        end
        % get correlation coefficients
        corr = corrcoef(dataWnd); % this is a 2x2 matrix
        if size(corr,1) == 1
            feature(start_idx,:)= corr(1,1);
        else
            feature(start_idx,:)= corr(1,2);
        end
        
    case 'coh'
        if size(dataWnd,2)~=2
            error 'feature "mutinf" only implemented for 2 column data';
        end
        % get correlation coefficients
        %feature(start_idx,:)= intcoh(dataWnd(:,1),dataWnd(:,2),3,20); % Andreas' implementation

        % MATLAB functions
        segments=3;
        Cxy = mscohere(dataWnd(:,1),dataWnd(:,2), ...
                       hanning(round(size(dataWnd,1)/segments)),... % window
                       [],... % noverlap, default: overlap 50%
                       [],... % nfft
                       20 ); % sampling frequency
        feature(start_idx,:) = sum(Cxy)/length(Cxy); % max frequency is 10 Hz - sum up all
    end;        
end;
    
if isempty(feature), disp('Warning: Empty featurevector');end;

