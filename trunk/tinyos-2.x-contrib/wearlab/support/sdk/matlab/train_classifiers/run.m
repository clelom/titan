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
% this file reads the complete data, generates features for given 
% window sizes, and stores them into an ARFF file for rapidminer.
%
% strDataFile   data file as produced in run_marker
% strLabelFile  label file produced by marker
%
function run(strDataFile,strLabelFile)


runOnce( strDataFile,strLabelFile,20, 0, {'mean', 'var', 'max', 'min'} );

%for i=10:20
%    fprintf(1,'************ Running for shift %i\n', i );
%    %runOnce( 80, i, {'corr','coh'},{'mag'} );
%    runOnce( 20, i, {'mean', 'var', 'median', 'zeroxing', 'max', 'min'},{'magX','magY' } );
%    %runOnce( 20, i, {'zeroxing'},{'magX','magY' } );
%end


function runOnce(strDataFile,strLabelFile,windowsize,shift,features)

%%
% originally cut off in sequence:
load(strDataFile);
load(strLabelFile);

% collect acceleration data and compute magnitude
data=[];
datanames = cell(0);
for i=1:length(logs)
    datanames{length(datanames)+1} = sprintf('%u_x',i);
    datanames{length(datanames)+1} = sprintf('%u_y',i);
    datanames{length(datanames)+1} = sprintf('%u_z',i);
    datanames{length(datanames)+1} = sprintf('%u_mag',i);
    
    data = [data logs{i}.data  sqrt(sum(logs{i}.data .* logs{i}.data,2))];
end

% create labels
datalabels = segments2labeling(seg); % must be single column
datalabels = [datalabels; zeros(size(data,1)-size(datalabels,1),1)];

% shift data to see what happens
%data = [ data(1:end-shift,1) data(shift+1:end,2) ];
%datalabels = [ datalabels(1:end-shift,1) ]; % keep the label of one of the groups


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% run

% compute features
featuredata=[];
labels=[];
for i=1:length(datanames) % this is not optimal - feature_slidingwindow can compute on multiple columns
    
    for j=1:length(features)
        idx = (i-1)*length(features)+j;
        featurenames{idx} = sprintf('%s_%s', datanames{i}, features{j});
        fprintf(1,'Computing %s...',featurenames{idx});
        if strcmp(features{j},'corr') || strcmp(features{j},'coh')
            [ a labels ] = feature_slidingwindow(data(:,1:2),features{j},windowsize,datalabels);
        else
            [ a labels ] = feature_slidingwindow(data(:,i),features{j},windowsize,datalabels);
        end
        featuredata = [featuredata a];
        fprintf(1,'\tdone\n');
    end
end

%% test synchronization
% slabels = labels;
% sfeaturedata = featuredata;
% 
% figure;
% for i=1:3
%     subplot(3,1,i), plot([sfeaturedata(:,[2 10]) slabels]);
%     slabels = slabels(windowsize:end,:);
%     sfeaturedata = sfeaturedata(1:end-windowsize+1,:);
% end

%figure;
%for j=1:8
%    subplot(4,2,j), plot([featuredata(:,j) labels]);
%    title(features{j});
%end

labels = labels(windowsize:end,:);
featuredata = featuredata(1:end-windowsize+1,:);



%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%% Converting to ARFF

fprintf(1,'Writing ARFF file...');
% open and write header
fid = fopen(sprintf('%s_w%03i_s%03i.arff',strrep(strDataFile,'.mat',''),windowsize,shift),'w');
fprintf( fid, '@relation dice_correlation\n' );
fprintf( fid, '  @attribute time real\n' );

for i=1:length(featurenames)
    fprintf(fid,'  @attribute %s numeric\n', featurenames{i});
end

fprintf(fid,'  @attribute label {1,2}\n');

fprintf( fid, '\n@data\n' );

fprintf(1,'\t  0%%');
% write data
for i=1:size(featuredata,1)
    fprintf(fid,'%d ',i); % save time
    
    % save all feature values
    for j=1:size(featuredata,2)
        fprintf(fid,'%f ', featuredata(i,j));
    end
	
	fprintf(fid,'%d', labels(i));
    
    fprintf(fid,'\n');
    
    if mod(i,round(size(featuredata,1)/20)) == 0
        fprintf(1,'\b\b\b\b%3u%%',round(100*i/size(featuredata,1)));
    end
    
    
    
end

fclose(fid);

fprintf(1,'\b\b\b\bdone');

